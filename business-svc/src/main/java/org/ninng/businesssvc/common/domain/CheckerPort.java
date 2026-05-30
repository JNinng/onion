package org.ninng.businesssvc.common.domain;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.entity.exception.PermissionsException;

import java.util.List;

public interface CheckerPort {

    void checkOwnerUser(@NonNull List<Long> changeRoleIds) throws PermissionsException;

    void checkOwnerUser(@NonNull List<Long> changeRoleIds, @NonNull UserDTO userDTO) throws PermissionsException;
}
