package org.ninng.businesssvc.identity.domain.service;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.CheckerPort;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleCheckerPortImpl implements CheckerPort {

    @Override
    public boolean checkOwnerUser(@NonNull List<Long> changeRoleIds, UserDTO user) {
        return checkOwnerUser(changeRoleIds, user.getUserDetails(), user.getRoles());
    }

    @Override
    public boolean checkOwnerUser(@NonNull List<Long> changeRoleIds, UserDetailsView user,
                                  @NonNull List<RoleDetailsView> roles) {
        if (user == null) {
            return false;
        }
        return true;
    }
}
