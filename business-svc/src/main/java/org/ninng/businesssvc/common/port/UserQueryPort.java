package org.ninng.businesssvc.common.port;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.exception.UserNotFoundException;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;

import java.util.List;

public interface UserQueryPort {

    UserDTO findByUsername(String username) throws UserNotFoundException;

    UserDetailsView userDetailsByUsername(String username) throws UserNotFoundException;

    @NonNull
    List<RoleDetailsView> roleDetailsByUserId(long userId);
}
