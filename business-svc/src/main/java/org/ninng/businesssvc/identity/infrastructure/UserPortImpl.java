package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTable;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户端口接口的 Jimmer 持久化实现。
 *
 * <p>继承 {@link CommonRepository} 复用通用 CRUD 和分页查询能力。
 * 提供多种 {@code select} 重载方法以满足不同查询粒度需求
 * （View 视图查询 / Fetcher 抓取器查询 / 规格过滤查询 / 分页查询）。</p>
 *
 * @author onion
 */
@Repository
public class UserPortImpl extends CommonRepository<SysUser, Long> implements UserPort {

    /**
     * Jimmer 编译期生成的用户表定义
     */
    private static final SysUserTable table = SysUserTable.$;

    /**
     * 通过 Jimmer SQL 客户端构造用户持久化实现。
     *
     * @param sql Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public UserPortImpl(JSqlClient sql) {
        super(sql);
    }

    /**
     * 返回此仓储对应的 Jimmer 表定义对象，供父类 {@link CommonRepository} 使用。
     *
     * @return {@link SysUserTable} 表定义
     */
    @Override
    public AbstractTypedTable<SysUser> getTable() {
        return table;
    }

    /**
     * 根据用户名精确查询用户详情视图。
     *
     * <p>查询结果会被缓存到 Caffeine（L1）和 Redis（L2）两级缓存中，
     * 缓存名称为 {@code "u"}，以用户名作为缓存键。</p>
     *
     * @param username 用户名（登录名），不可为 {@code null}
     * @return 匹配的用户详情视图，未找到时返回 {@code null}
     */
    @Nullable
    @Override
    @Cacheable(cacheNames = "u", key = "#username", unless = "#result==null")
    public UserDetailsView findByUsername(String username) {
        List<UserDetailsView> list = createQuery().where(table.name()
                        .eq(username))
                .select(table.fetch(UserDetailsView.class))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    /**
     * 更新用户信息。
     *
     * <p>通过 {@code UserUpdateInput} 动态实体提交变更，
     * Jimmer 自动生成差分 SQL，仅更新实际修改的字段。</p>
     *
     * @param input 用户更新输入（Jimmer 动态实体）
     * @return {@code true} 表示数据已修改并持久化，{@code false} 表示无变更
     */
    @Override
    public Boolean update(UserUpdateInput input) {
        return sql.saveCommand(input)
                .execute()
                .isModified();
    }

    /**
     * 注册新用户（仅插入模式）。
     *
     * @param input 用户输入数据（Jimmer 动态实体）
     * @return 注册成功后的用户实体
     */
    @Override
    public SysUser register(Input<SysUser> input) {
        // 使用 INSERT_ONLY 模式确保只插入、不更新已有记录
        return saveCommand(input).setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY)
                .execute()
                .getModifiedEntity();
    }

    /**
     * 查询所有用户，使用指定视图类进行字段投影。
     *
     * @param viewClass Jimmer View 类，控制返回对象的字段集合
     * @param <V>       视图类型，必须继承 {@link View}{@code <SysUser>}
     * @return 用户视图列表
     */
    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass) {
        return super.select(viewClass);
    }

    /**
     * 查询所有用户，使用指定抓取器控制字段加载。
     *
     * @param fetcher 对象抓取器
     * @return 用户实体列表
     */
    @Override
    public List<SysUser> select(Fetcher<SysUser> fetcher) {
        return super.select(fetcher);
    }

    /**
     * 查询所有用户，支持视图投影和规格过滤，按创建时间倒序排列。
     *
     * @param viewClass     Jimmer View 类，控制返回对象的字段集合
     * @param specification 用户查询规格，用于构建动态过滤条件
     * @param <V>           视图类型
     * @return 匹配的用户视图列表，按创建时间倒序
     */
    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification) {
        // 按创建时间倒序排列，最新创建的用户排在前面
        return createQuery().where(specification)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    /**
     * 分页查询用户列表，支持动态规格过滤。
     *
     * @param fetcher       对象抓取器
     * @param pageReq       分页请求参数
     * @param specification 用户查询规格
     * @return Jimmer 分页结果
     */
    @Override
    public Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }

    /**
     * 统计给定用户 ID 列表中可见（未被过滤）的用户数量。
     *
     * <p>用于校验待操作用户是否存在且对当前上下文可见。
     * "可见"受 Jimmer 过滤拦截器（如多租户、数据权限）影响。</p>
     *
     * @param userIds 用户 ID 列表，不可为 {@code null}
     * @return 可见的用户数量
     */
    @Override
    public long countVisible(@NonNull List<Long> userIds) {
        return createQuery().where(table.id()
                        .in(userIds))
                .selectCount()
                .execute()
                .getFirst();
    }
}
