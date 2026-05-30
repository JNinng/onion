package org.ninng.businesssvc.identity.domain.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.CheckerPort;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.PermissionsException;
import org.ninng.businesssvc.identity.domain.port.RolePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RoleCheckerPortImpl implements CheckerPort {

    private final I18nUtil i18n;
    private final RolePort rolePort;

    public RoleCheckerPortImpl(I18nUtil i18n, RolePort rolePort) {
        this.i18n = i18n;
        this.rolePort = rolePort;
    }

    @Override
    public void checkOwnerUser(@NonNull List<Long> changeRoleIds) throws PermissionsException {
        checkOwnerUserHandler(changeRoleIds);
    }

    @Override
    public void checkOwnerUser(@NonNull List<Long> changeRoleIds,
                               @NonNull UserDTO userDTO) throws PermissionsException {
        val user = userDTO.getUserDetails();
        try {
            val allow = UserContextHolder.withSnapshot(
                    new UserContextHolder.Snapshot(user.getTenantId(), user, user.getOwnerDeptId(), userDTO.getRoles()),
                    () -> checkOwnerUserHandler(changeRoleIds));
            if (!allow) {
                throw new PermissionsException(i18n.getMessage("exception.notDataPermissions"));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    boolean checkOwnerUserHandler(@NonNull List<Long> changeRoleIds) {
        if (changeRoleIds.isEmpty()) {
            return false;
        }
        return rolePort.countVisible(changeRoleIds) > changeRoleIds.size();
    }
}
