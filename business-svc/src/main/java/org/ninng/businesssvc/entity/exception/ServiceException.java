package org.ninng.businesssvc.entity.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceException extends BizException {

    @Serial
    private static final long serialVersionUID = -7483165571012985401L;

    public ServiceException(String message) {
        super(message, ErrCode.SERVICE_EXCEPTION);
    }
}
