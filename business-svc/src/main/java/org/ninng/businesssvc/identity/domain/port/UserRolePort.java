package org.ninng.businesssvc.identity.domain.port;

import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;

import java.util.List;

/**
 * 用户角色关联端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对用户与角色多对多关联关系所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供，应用层仅依赖此接口，不感知底层存储技术。
 * <p>
 * <p>主要职责：按用户 ID 查询其关联的角色列表，返回角色详情视图。
 * 该端口抽象了 {@code user_role_mapping} 关联表的查询逻辑。
 * <p>
 * <p>关联表结构：用户与角色之间通过 {@code user_role_mapping} 表建立多对多关联，
 * 该表仅包含 {@code userId} 和 {@code roleId} 两个业务字段，不包含审计字段。
 */
public interface UserRolePort {

    /**
     * 根据用户 ID 查询其关联的角色列表。
     * <p>
     * <p>默认启用租户过滤：仅返回当前租户下的角色。
     * <p>前置条件：{@code userId} 可以为 {@code null}，此时返回空列表。
     *
     * @param userId 用户 ID，可以为 {@code null}
     * @return 该用户关联的角色详情视图列表；若用户 ID 为 {@code null} 或无关联角色则返回空列表
     */
    List<RoleDetailsView> findByUserId(@Nullable Long userId);

    /**
     * 根据用户 ID 查询其关联的角色列表，可控制是否禁用租户过滤。
     * <p>
     * <p>当 {@code disableTenant} 为 {@code true} 时，跳过租户过滤，返回该用户
     * 在所有租户下关联的角色（用于跨租户管理场景）。
     *
     * @param userId        用户 ID，可以为 {@code null}
     * @param disableTenant 是否禁用租户过滤；{@code true} 禁用，{@code false} 启用
     * @return 该用户关联的角色详情视图列表；若用户 ID 为 {@code null} 或无关联角色则返回空列表
     */
    List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant);
}
