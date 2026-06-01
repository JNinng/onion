package org.ninng.businesssvc.identity.domain.service;

import lombok.val;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.common.domain.port.UserQueryPort;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.UserNotFoundException;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户查询领域服务。
 *
 * <p>实现 {@link UserQueryPort} 接口，负责根据用户名查询用户信息并组装完整的 {@link UserDTO}。
 * 组合了 {@link UserPort}（用户数据）和 {@link UserRolePort}（用户角色关联数据）两个端口，
 * 将分散的数据聚合成便于上层使用的统一 DTO。</p>
 *
 * <p><b>权限隔离</b>：查询用户详情和角色时，通过
 * {@link UserContextHolder#withOwnerDisabled} 临时关闭数据所有者过滤，
 * 确保查询逻辑不受当前用户的数据权限限制。</p>
 *
 * @see UserDTO
 * @see UserPort
 * @see UserRolePort
 */
@Service
public class UserQueryPortImpl implements UserQueryPort {

    /**
     * 用户数据访问端口
     */
    private final UserPort userPort;
    /**
     * 用户角色关联数据访问端口
     */
    private final UserRolePort userRolePort;

    /**
     * 构造函数，通过 Spring 依赖注入初始化。
     *
     * @param userPort     用户数据访问端口
     * @param userRolePort 用户角色关联数据访问端口
     */
    public UserQueryPortImpl(UserPort userPort, UserRolePort userRolePort) {
        this.userPort = userPort;
        this.userRolePort = userRolePort;
    }

    /**
     * 根据用户名查询完整的用户 DTO。
     *
     * <p>整合了两个数据源：</p>
     * <ol>
     *   <li>通过 {@link #userDetailsByUsername(String)} 获取用户基本信息</li>
     *   <li>通过 {@link #roleDetailsByUserId(long)} 获取用户的角色列表</li>
     * </ol>
     *
     * @param username 用户名
     * @return 包含用户详情和角色列表的 {@link UserDTO}
     * @throws UserNotFoundException 如果用户不存在
     */
    @Override
    public UserDTO findByUsername(String username) throws UserNotFoundException {
        // 先查用户基本信息
        val detailsView = userDetailsByUsername(username);
        // 再查用户关联的角色列表
        val roleDetailsViews = roleDetailsByUserId(detailsView.getId());
        // 组装完整 DTO
        return new UserDTO(detailsView, roleDetailsViews);
    }

    /**
     * 根据用户名查询用户详情。
     *
     * <p>查询时临时关闭数据所有者过滤（{@link UserContextHolder#withOwnerDisabled}），
     * 确保查询不受当前用户上下文的数据范围限制。</p>
     *
     * @param username 用户名
     * @return 用户详情视图
     * @throws UserNotFoundException 如果用户不存在
     */
    @Override
    public UserDetailsView userDetailsByUsername(String username) throws UserNotFoundException {
        // 关闭所有者过滤后查询，避免数据权限干扰
        val detailsView = UserContextHolder.withOwnerDisabled(() -> (userPort.findByUsername(username)));
        // 用户不存在时抛出业务异常
        if (detailsView == null) {
            throw new UserNotFoundException(username);
        }
        return detailsView;
    }

    /**
     * 根据用户 ID 查询关联的角色列表。
     *
     * <p>查询时临时关闭数据所有者过滤，确保获取完整的角色关联信息。</p>
     *
     * @param userId 用户 ID
     * @return 角色详情视图列表，保证非空
     */
    @Override
    @NonNull
    public List<RoleDetailsView> roleDetailsByUserId(long userId) {
        // 关闭所有者过滤后查询用户角色关联
        return UserContextHolder.withOwnerDisabled(() -> userRolePort.findByUserId(userId));
    }
}
