package org.ninng.businesssvc.identity.domain.service;

import lombok.val;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.common.port.UserQueryPort;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.UserNotFoundException;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserQueryPortImpl implements UserQueryPort {

    private final UserPort userPort;
    private final UserRolePort userRolePort;

    public UserQueryPortImpl(UserPort userPort, UserRolePort userRolePort) {
        this.userPort = userPort;
        this.userRolePort = userRolePort;
    }

    @Override
    public UserDTO findByUsername(String username) throws UserNotFoundException {
        val detailsView = userDetailsByUsername(username);
        val roleDetailsViews = roleDetailsByUserId(detailsView.getId());
        return new UserDTO(detailsView, roleDetailsViews);
    }

    @Override
    public UserDetailsView userDetailsByUsername(String username) throws UserNotFoundException {
        val detailsView = UserContextHolder.withOwnerDisabled(() -> (userPort.findByUsername(username)));
        if (detailsView == null) {
            throw new UserNotFoundException(username);
        }
        return detailsView;
    }

    @Override
    @NonNull
    public List<RoleDetailsView> roleDetailsByUserId(long userId) {
        return UserContextHolder.withOwnerDisabled(() -> userRolePort.findByUserId(userId));
    }
}
