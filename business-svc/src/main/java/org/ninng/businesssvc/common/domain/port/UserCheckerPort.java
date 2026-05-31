package org.ninng.businesssvc.common.domain.port;

import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.common.domain.model.UserDTO;
import org.ninng.businesssvc.entity.exception.PermissionsException;

import java.util.List;

public interface UserCheckerPort {

    void checkVisible(@NonNull Long userId) throws PermissionsException;

    void checkVisible(@NonNull List<Long> userIds) throws PermissionsException;

    void checkVisible(@NonNull List<Long> userIds, @NonNull UserDTO userDTO) throws PermissionsException;
}
