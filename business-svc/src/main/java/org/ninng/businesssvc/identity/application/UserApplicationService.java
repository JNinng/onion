package org.ninng.businesssvc.identity.application;

import lombok.val;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.common.domain.port.RoleCheckerPort;
import org.ninng.businesssvc.common.domain.port.UserCheckerPort;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.context.UserContextMode;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.*;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.ninng.businesssvc.utils.CollectionDiffUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 用户应用服务，负责处理用户管理的业务用例，包括注册、更新、分页查询和选项列表。
 * <p>
 * <p>该服务协调以下端口组件完成业务逻辑：
 * <ul>
 * <li>{@link UserPort} — 用户数据持久化端口</li>
 * <li>{@link UserCheckerPort} — 用户可见性校验端口</li>
 * <li>{@link RoleCheckerPort} — 角色可见性校验端口</li>
 * <li>{@link UserRolePort} — 用户角色关联查询端口</li>
 * </ul>
 * <p>
 * <p>在更新用户时，会对用户 ID 和角色 ID 进行可见性校验，
 * 并通过 {@link CollectionDiffUtils} 计算角色变更的差异集，仅对变更的角色执行权限检查，
 * 避免不必要的数据库查询。
 * <p>
 * <p>主要用例：
 * <ul>
 * <li>用户注册</li>
 * <li>更新用户信息及角色关联（含权限校验）</li>
 * <li>分页查询用户列表</li>
 * <li>获取用户选项列表（用于下拉选择等场景）</li>
 * </ul>
 *
 * @see UserPort
 * @see UserCheckerPort
 * @see RoleCheckerPort
 * @see CollectionDiffUtils
 */
@Service
public class UserApplicationService {

    private final UserPort userPort;
    private final UserCheckerPort userCheckerPort;
    private final RoleCheckerPort roleCheckerPort;
    private final UserRolePort userRolePort;

    public UserApplicationService(UserPort userPort, UserCheckerPort userCheckerPort, RoleCheckerPort roleCheckerPort,
                                  UserRolePort userRolePort) {
        this.userPort = userPort;
        this.userCheckerPort = userCheckerPort;
        this.roleCheckerPort = roleCheckerPort;
        this.userRolePort = userRolePort;
    }

    /**
     * 注册新用户。
     * <p>
     * <p>注意：密码哈希处理由上游调用方（通常是 {@link AuthApplicationService#register(RegisterInput)}）
     * 在调用此方法前完成，此处直接委托给端口层持久化。
     *
     * @param registerInput 注册输入，包含用户名、已哈希的密码等信息
     * @return 注册成功后的 {@link SysUser} 实体
     */
    public SysUser register(RegisterInput registerInput) {
        return userPort.register(registerInput);
    }

    /**
     * 更新用户信息及其角色关联。
     * <p>
     * <p>更新流程：
     * <ol>
     * <li>校验用户 ID 不为空</li>
     * <li>校验当前操作者对目标用户的可见性</li>
     * <li>若传入了角色 ID 列表，则：
     * <ul>
     * <li>以禁用租户过滤的模式查询用户现有角色（跨租户查询）</li>
     * <li>使用 {@link CollectionDiffUtils#diff(Collection, Collection)} 计算角色变更的差异集</li>
     * <li>仅对新增的角色执行可见性校验，减少不必要的数据库查询</li>
     * </ul>
     * </li>
     * <li>委托 {@link UserPort#update(UserUpdateInput)} 完成持久化</li>
     * </ol>
     *
     * @param input 用户更新输入，包含用户 ID、新属性值及可选的角色 ID 列表
     * @return {@code true} 更新成功；{@code false} 更新失败（如用户 ID 为空）
     */
    public Boolean update(UserUpdateInput input) {
        // 用户 ID 为空时直接返回失败
        if (input.getId() == null) {
            return false;
        }
        // 校验当前操作者对目标用户的可见性
        userCheckerPort.checkVisible(input.getId());
        val roleIds = input.getRoleIds();
        if (!roleIds.isEmpty()) {
            // 以禁用租户过滤的模式查询用户现有角色，获取全量角色用于差异对比
            val oldRoleIds = UserContextHolder.withMode(UserContextMode.DisabledType.INSTANCE,
                            () -> userRolePort.findByUserId(input.getId()))
                    .stream()
                    .map(RoleDetailsView::getId)
                    .toList();
            // 计算角色变更的差异集：找出新增和移除的角色
            val diff = CollectionDiffUtils.diff(oldRoleIds, roleIds);
            // 仅对变更的角色进行可见性校验，避免不必要的全量校验
            roleCheckerPort.checkVisible(diff.changed());
        }
        return userPort.update(input);
    }

    /**
     * 分页查询用户列表，支持按条件筛选。
     *
     * @param fetcher       Jimmer 抓取器，用于控制返回的 {@link SysUser} 对象加载哪些关联字段
     * @param pageReq       分页请求参数，包含页码和每页条数
     * @param specification 用户查询规格，包含可选的筛选条件（如用户名、状态等）
     * @return 分页结果 {@link Page}，包含用户列表和分页信息
     */
    public Page<SysUser> list(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification) {
        return userPort.select(fetcher, pageReq, specification);
    }

    /**
     * 获取用户选项列表，用于前端下拉选择等场景。
     * <p>
     * <p>返回类型为 {@link UserSelectionView}，这是一个精简的用户视图，
     * 通常只包含 ID 和名称等必要字段，适合批量加载和列表展示。
     *
     * @return 用户选项列表，每个元素为 {@link UserSelectionView}
     */
    public List<UserSelectionView> selections() {
        return userPort.select(UserSelectionView.class);
    }
}
