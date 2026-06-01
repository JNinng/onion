package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;

import java.util.List;

/**
 * 用户端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对用户聚合根（{@link SysUser}）所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供，应用层仅依赖此接口，不感知底层存储技术。
 * <p>
 * <p>主要职责：
 * <ul>
 * <li>用户注册与信息更新</li>
 * <li>按用户名查询用户详情（用于身份认证）</li>
 * <li>用户列表查询（支持普通查询和分页查询）</li>
 * <li>用户可见性校验（用于权限变更前的合法性检查）</li>
 * </ul>
 * <p>
 * <p>注意：密码字段默认不在抓取范围内，需显式指定 {@code fetcher} 才会加载。
 */
public interface UserPort {

    /**
     * 根据用户名查询用户详情。
     * <p>
     * <p>典型用途：在身份认证流程中加载用户凭据、角色列表等安全信息。
     * 返回的 {@link UserDetailsView} 是 Jimmer 视图对象，字段根据视图定义预先确定。
     *
     * @param username 用户名（唯一标识）
     * @return 用户详情视图；若不存在则返回 {@code null}
     */
    @Nullable
    UserDetailsView findByUsername(String username);

    /**
     * 更新已有用户的信息。
     * <p>
     * <p>前置条件：{@code input} 中的用户 ID 必须对应一个已存在的用户。
     * <p>后置条件：用户的指定字段被更新为 {@code input} 中的值。
     * <p>只更新 {@link UserUpdateInput} 中非 {@code null} 的字段，实现部分更新语义。
     * 角色变更会触发关联表同步。
     *
     * @param input 用户更新输入对象，包含用户 ID 及待更新的字段
     * @return {@code true} 更新成功；{@code false} 用户不存在或更新失败
     */
    Boolean update(UserUpdateInput input);

    /**
     * 注册新用户。
     * <p>
     * <p>前置条件：传入的 {@code input} 必须通过 Jimmer 实体的校验规则，用户名不可重复。
     * <p>后置条件：返回的 {@link SysUser} 对象已包含生成的用户 ID（雪花算法）及所有请求字段。
     * 用户 ID 由 {@link org.ninng.businesssvc.model.SnowflakeIdGenerator} 生成。
     *
     * @param input 用户实体输入对象，包含待注册的用户数据
     * @return 新注册的用户实体
     */
    SysUser register(org.babyfish.jimmer.Input<SysUser> input);

    /**
     * 查询全部用户（无筛选条件）。
     * <p>
     * <p>返回类型为 {@link View} 的实现类，字段由视图定义预先确定。
     * 适用于下拉列表等不需要分页的场景。
     *
     * @param <V>       视图类型，必须为 {@link View}{@code <SysUser>} 的子类型
     * @param viewClass 视图类，指定返回对象的字段集合
     * @return 用户视图列表，可能为空列表
     */
    <V extends View<SysUser>> List<V> select(Class<V> viewClass);

    /**
     * 按条件查询用户列表（View 形式）。
     * <p>
     * <p>支持通过 {@link UserSpecification} 进行多条件筛选。
     * 适用于下拉列表等不需要分页但需要筛选的场景。
     *
     * @param <V>           视图类型，必须为 {@link View}{@code <SysUser>} 的子类型
     * @param viewClass     视图类，指定返回对象的字段集合
     * @param specification 用户查询规格，包含筛选条件（如用户名、状态、部门 ID 等）
     * @return 符合条件的用户视图列表，可能为空列表
     */
    <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification);

    /**
     * 查询全部用户（实体形式，无筛选条件）。
     * <p>
     * <p>通过 {@link Fetcher} 精确控制加载的字段，比 {@link View} 更灵活。
     *
     * @param fetcher Jimmer 对象抓取器，指定需要加载的字段
     * @return 用户实体列表，可能为空列表
     */
    List<SysUser> select(Fetcher<SysUser> fetcher);

    /**
     * 分页查询用户列表。
     * <p>
     * <p>支持通过 {@link UserSpecification} 进行多条件筛选，结果按分页参数 {@link PageReq} 返回。
     * <p>查询自动应用租户隔离：仅返回当前租户下的用户数据。
     *
     * @param fetcher       Jimmer 对象抓取器，指定需要加载的字段
     * @param pageReq       分页请求参数（页码、每页条数、排序）
     * @param specification 用户查询规格，包含筛选条件（如用户名、昵称、状态、部门 ID 等）
     * @return 分页的用户实体列表
     */
    Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification);

    /**
     * 统计当前用户可见的用户数量。
     * <p>
     * <p>用于在用户相关操作前进行权限校验，确保操作的目标用户均在该用户的可见范围内。
     * 可见性由租户归属和数据权限共同决定。
     * <p>前置条件：{@code userIds} 列表非 {@code null}。
     * <p>后置条件：返回的数量等于 {@code userIds} 的大小时，表示所有目标用户均可见。
     *
     * @param userIds 待校验的用户 ID 列表，不可为 {@code null}
     * @return 在当前用户租户和数据权限范围内可见的用户数量
     */
    long countVisible(@NonNull List<Long> userIds);
}
