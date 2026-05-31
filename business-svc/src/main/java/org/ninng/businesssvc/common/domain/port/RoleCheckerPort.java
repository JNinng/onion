package org.ninng.businesssvc.common.domain.port;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.entity.exception.PermissionsException;

import java.util.List;

public interface RoleCheckerPort {

    void checkVisible(@NonNull Long roleId) throws PermissionsException;

    void checkVisible(@NonNull List<Long> roleIds) throws PermissionsException;

    void checkVisible(@NonNull List<Long> roleIds, @NonNull UserDTO userDTO) throws PermissionsException;
}
