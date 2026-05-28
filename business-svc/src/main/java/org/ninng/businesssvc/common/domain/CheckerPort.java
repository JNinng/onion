package org.ninng.businesssvc.common.domain;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;

import java.util.List;

public interface CheckerPort {

    boolean checkOwnerUser(@NonNull List<Long> changeRoleIds, UserDTO user);

    boolean checkOwnerUser(@NonNull List<Long> changeRoleIds, UserDetailsView user,
                           @NonNull List<RoleDetailsView> roles);
}
