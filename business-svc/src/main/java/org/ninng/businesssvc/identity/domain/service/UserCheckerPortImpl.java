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

@Slf4j
@Service
public class UserCheckerPortImpl implements UserCheckerPort {

    private final I18nUtil i18n;
    private final UserPort userPort;

    public UserCheckerPortImpl(I18nUtil i18n, UserPort userPort) {
        this.i18n = i18n;
        this.userPort = userPort;
    }

    @Override
    public void checkVisible(@NonNull Long userId) throws PermissionsException {
        checkVisible(List.of(userId));
    }

    @Override
    public void checkVisible(@NonNull List<Long> userIds) throws PermissionsException {
        if (!checkerVisibleHandle(userIds)) {
            throw new PermissionsException(i18n.getMessage("exception.notDataPermissions"));
        }
    }

    @Override
    public void checkVisible(@NonNull List<Long> userIds, @NonNull UserDTO userDTO) throws PermissionsException {
        val user = userDTO.getUserDetails();
        try {
            val allow = UserContextHolder.withSnapshot(
                    new UserContextHolder.Snapshot(user.getTenantId(), user, user.getOwnerDeptId(), userDTO.getRoles()),
                    () -> checkerVisibleHandle(userIds)
            );
            if (!allow) {
                throw new PermissionsException(i18n.getMessage("exception.notDataPermissions"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    boolean checkerVisibleHandle(@NonNull List<Long> changeUserIds) throws PermissionsException {
        if (changeUserIds.isEmpty()) {
            return false;
        }
        return userPort.countVisible(changeUserIds) == changeUserIds.size();
    }
}
