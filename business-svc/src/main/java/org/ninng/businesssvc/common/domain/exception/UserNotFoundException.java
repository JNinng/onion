package org.ninng.businesssvc.common.domain.exception;

import java.io.Serial;

public class UserNotFoundException extends TenantException {

    @Serial
    private static final long serialVersionUID = -4940735696276415339L;

    public UserNotFoundException(String message) {
        super(message, ErrCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message, String tenantId) {
        super(message, tenantId, ErrCode.USER_NOT_FOUND);
    }
}
