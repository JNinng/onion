package org.ninng.businesssvc.identity.domain.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.common.domain.port.UserCheckerPort;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.PermissionsException;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户可见性校验领域服务。
 *
 * <p>实现 {@link UserCheckerPort} 接口，负责判断当前用户是否有权限查看指定用户。
 * 核心逻辑通过 {@link UserPort#countVisible(List)} 统计可见用户数量，
 * 与传入的用户 ID 集合大小比对，确保每个目标用户都对当前用户可见。</p>
 *
 * <p>支持三种校验场景：</p>
 * <ul>
 *   <li><b>当前用户上下文校验</b> — 直接使用 {@link UserContextHolder} 中的当前用户信息</li>
 *   <li><b>指定用户上下文校验</b> — 通过 {@link UserContextHolder#withSnapshot} 切换到指定用户快照再校验</li>
 * </ul>
 *
 * @see UserContextHolder
 * @see UserPort
 */
@Slf4j
@Service
public class UserCheckerPortImpl implements UserCheckerPort {

    /**
     * 国际化消息工具
     */
    private final I18nUtil i18n;
    /**
     * 用户数据访问端口
     */
    private final UserPort userPort;

    /**
     * 构造函数，通过 Spring 依赖注入初始化。
     *
     * @param i18n     国际化消息工具
     * @param userPort 用户数据访问端口
     */
    public UserCheckerPortImpl(I18nUtil i18n, UserPort userPort) {
        this.i18n = i18n;
        this.userPort = userPort;
    }

    /**
     * 校验单个用户是否对当前用户可见。
     * <p>委托给 {@link #checkVisible(List)} 批量校验方法。</p>
     *
     * @param userId 用户 ID
     * @throws PermissionsException 如果用户不可见
     */
    @Override
    public void checkVisible(@NonNull Long userId) throws PermissionsException {
        // 转为单元素列表，委托批量校验方法
        checkVisible(List.of(userId));
    }

    /**
     * 批量校验用户是否对当前用户可见。
     * <p>使用当前 {@link UserContextHolder} 中的用户信息进行权限判定。</p>
     *
     * @param userIds 用户 ID 列表
     * @throws PermissionsException 如果任一用户不可见
     */
    @Override
    public void checkVisible(@NonNull List<Long> userIds) throws PermissionsException {
        // 调用核心校验逻辑，不通过则抛出权限异常
        if (!checkerVisibleHandle(userIds)) {
            throw new PermissionsException(i18n.getMessage("exception.notDataPermissions"));
        }
    }

    /**
     * 以指定用户身份批量校验用户是否可见。
     *
     * <p>通过 {@link UserContextHolder#withSnapshot} 临时切换到指定用户的上下文快照，
     * 在快照作用域内执行校验逻辑，校验完成后自动恢复原始上下文。</p>
     *
     * @param userIds 用户 ID 列表
     * @param userDTO 指定用户的 DTO，包含用户详情和角色信息
     * @throws PermissionsException 如果任一用户不可见或上下文切换异常
     */
    @Override
    public void checkVisible(@NonNull List<Long> userIds, @NonNull UserDTO userDTO) throws PermissionsException {
        // 提取用户详情
        val user = userDTO.getUserDetails();
        try {
            // 在指定用户的上下文快照中执行校验，隔离当前用户上下文
            val allow = UserContextHolder.withSnapshot(
                    new UserContextHolder.Snapshot(user.getTenantId(), user, user.getOwnerDeptId(), userDTO.getRoles()),
                    () -> checkerVisibleHandle(userIds)
            );
            if (!allow) {
                throw new PermissionsException(i18n.getMessage("exception.notDataPermissions"));
            }
        } catch (Exception e) {
            // 记录异常日志并原样抛出
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 核心校验逻辑：对比可见用户数量与请求校验的用户数量是否一致。
     *
     * <p>如果两个数量相等，说明所有待校验用户在当前用户上下文中均可见；否则说明存在不可见的用户。</p>
     *
     * @param changeUserIds 待校验的用户 ID 集合
     * @return {@code true} 所有用户可见；{@code false} 存在不可见用户或列表为空
     * @throws PermissionsException 当前不支持此场景
     */
    boolean checkerVisibleHandle(@NonNull List<Long> changeUserIds) throws PermissionsException {
        // 空列表直接返回不可见
        if (changeUserIds.isEmpty()) {
            return false;
        }
        // 对比可见用户数量与目标数量：全部可见才返回 true
        return userPort.countVisible(changeUserIds) == changeUserIds.size();
    }
}
