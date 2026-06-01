package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.ninng.businesssvc.identity.domain.type.DataScope;
import org.ninng.businesssvc.identity.domain.type.RoleType;
import org.ninng.businesssvc.model.SnowflakeIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

import java.util.List;

/**
 * 系统角色实体
 * <p>
 * <p>定义RBAC权限模型中的角色概念。每个角色包含角色类型（{@link RoleType}）、
 * 数据范围（{@link DataScope}）以及组织可见范围等核心属性，
 * 控制用户在系统中的操作权限和数据访问边界。</p>
 * <p>
 * <p>角色分为两类：</p>
 * <ul>
 * <li>系统预置角色 — 由平台定义，不可删除，核心权限不可剥离</li>
 * <li>租户自定义角色 — 由租户管理员自行创建和管理</li>
 * </ul>
 * <p>
 * <p>与{@link SysRoleIdScope}为一对多关联，
 * 通过 {@code roleIdScopes} 存储角色可访问的具体数据ID列表。</p>
 * <p>
 * <p>混入特性：</p>
 * <ul>
 * <li>{@link CreatedAware} — 记录创建时间与创建人</li>
 * <li>{@link UpdatedAware} — 记录更新时间与更新人</li>
 * <li>{@link StatusAware} — 支持逻辑删除（软删除）</li>
 * <li>{@link TenantAware} — 多租户隔离，每个角色归属特定租户</li>
 * </ul>
 */
@Entity
public interface SysRole extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * 角色唯一标识
     * <p>
     * <p>使用{@link SnowflakeIdGenerator}雪花算法生成全局唯一ID</p>
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 角色名
     */
    String name();

    /**
     * 权限编码
     * <p>
     * <p>用于权限控制判断的唯一编码，通常与前端路由或API权限标识对应</p>
     */
    String code();

    /**
     * 角色类型
     * <p>
     * <p>1-系统预置(不可删除，核心权限不可剥离) 2-租户自定义</p>
     */
    RoleType roleType();

    /**
     * 数据范围规则
     * <p>
     * <p>1-仅本人 2-本部门 3-本部门及子部门 4-指定人 5-指定部门 6-全租户</p>
     */
    DataScope dataScope();

    /**
     * 组织可见范围
     * <p>
     * <p>限定角色可分配的部门层级。{@code null}=全租户可分配。
     * 非空时表示仅允许在该部门及其下级部门中分配此角色。</p>
     */
    @Nullable
    Long scopeDeptId();

    /**
     * 备注
     */
    @Nullable
    String remark();

    /**
     * 角色关联的数据ID范围列表
     * <p>
     * <p>一对多关联到{@link SysRoleIdScope}，通过 {@code role} 字段映射。
     * 当角色的数据范围为「指定人」或「指定部门」时，
     * 此列表存储具体允许访问的用户ID或部门ID。</p>
     * <p>
     * <p>Jimmer {@code @OneToMany(mappedBy = "role")} 表示
     * 由 {@link SysRoleIdScope#role()} 端维护关联关系。</p>
     */
    @OneToMany(mappedBy = "role")
    List<SysRoleIdScope> roleIdScopes();
}
