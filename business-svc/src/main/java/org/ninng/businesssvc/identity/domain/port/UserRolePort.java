package org.ninng.businesssvc.identity.domain.port;

import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;

import java.util.List;

public interface UserRolePort {

    List<RoleDetailsView> findByUserId(@Nullable Long userId);

    List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant);
}
