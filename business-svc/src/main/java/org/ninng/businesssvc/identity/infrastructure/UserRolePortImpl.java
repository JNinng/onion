package org.ninng.businesssvc.identity.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.constant.C;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleTableEx;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTableEx;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.ninng.businesssvc.model.filter.TenantFilter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-角色关联端口接口的 Jimmer 持久化实现。
 *
 * <p>不同于其他 PortImpl，本类<strong>不继承</strong> {@link org.ninng.businesssvc.repository.CommonRepository}，
 * 而是直接使用 {@link JSqlClient} 操作 Jimmer 关联查询（{@link AssociationTable}）。
 * 通过 {@code SysUser} 与 {@code SysRole} 之间的多对多关联表进行跨实体查询。</p>
 *
 * <p>查询结果会被缓存到 Caffeine（L1）和 Redis（L2）两级缓存中，
 * 缓存名称为 {@code "ur"}，以用户 ID 作为缓存键。</p>
 *
 * @author onion
 */
@Repository
@Slf4j
public class UserRolePortImpl implements UserRolePort {

    /**
     * Jimmer SQL 客户端，直接操作数据库
     */
    private final JSqlClient sqlClient;

    /**
     * 通过 Jimmer SQL 客户端构造用户-角色关联持久化实现。
     *
     * @param sqlClient Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public UserRolePortImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    /**
     * 根据用户 ID 查询该用户拥有的所有有效角色。
     *
     * <p>默认不禁用租户过滤器，查询结果受多租户过滤拦截器影响。</p>
     *
     * @param userId 用户 ID，可为 {@code null}（返回空列表）
     * @return 角色详情视图列表，用户无角色或 ID 为 {@code null} 时返回空列表
     */
    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId) {
        // 委托到带租户开关的重载方法，默认启用租户过滤
        return findByUserId(userId, false);
    }

    /**
     * 根据用户 ID 查询该用户拥有的所有有效角色，支持控制租户过滤器开关。
     *
     * <p>通过 Jimmer {@code AssociationTable} 关联 {@code SysUser} 和 {@code SysRole}
     * 的多对多关系表，查询同时满足以下条件的角色：</p>
     * <ul>
     *     <li>用户 ID 匹配</li>
     *     <li>用户状态为启用（{@code ENABLED}）</li>
     *     <li>角色状态为启用（{@code ENABLED}）</li>
     * </ul>
     *
     * @param userId        用户 ID，可为 {@code null}（返回空列表）
     * @param disableTenant 设为 {@code true} 时禁用租户过滤器，适用于跨租户查询场景
     * @return 角色详情视图列表，用户无角色或 ID 为 {@code null} 时返回空列表
     */
    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant) {
        // 构建 SysUser 与 SysRole 之间的多对多关联表对象
        AssociationTable<SysUser, SysUserTableEx, SysRole, SysRoleTableEx> associationTable = AssociationTable.of(
                SysUserTableEx.class, SysUserTableEx::roles);
        try {
            List<RoleDetailsView> list = sqlClient.filters(it -> {
                        // 当需要跨租户查询时，禁用 TenantFilter 拦截器
                        if (disableTenant) {
                            it.disableByTypes(TenantFilter.class);
                        }
                    })
                    .createAssociationQuery(associationTable)
                    // 查询条件：用户 ID 匹配 + 双方状态均为启用
                    .where(Predicate.and(
                            associationTable.source()
                                    .id()
                                    .eq(userId),
                            associationTable.source()
                                    .status()
                                    .eq(C.Data.ENABLED),
                            associationTable.target()
                                    .status()
                                    .eq(C.Data.ENABLED)))
                    .select(associationTable.target()
                            .fetch(RoleDetailsView.class))
                    .execute();
            if (list == null) {
                return List.of();
            }
            return list;
        } catch (Exception e) {
            // 查询异常时记录日志并返回空列表，避免影响调用方
            log.error(e.getMessage(), e);
            return List.of();
        }
    }
}
