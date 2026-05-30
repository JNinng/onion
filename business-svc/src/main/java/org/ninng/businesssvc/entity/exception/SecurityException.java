package org.ninng.businesssvc.entity.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class SecurityException extends BizException {

    @Serial
    private static final long serialVersionUID = 342686615400929388L;

    private String algorithm;

    public SecurityException(String message, Throwable cause, String algorithm) {
        super(message, ErrCode.SECURITY_EXCEPTION, cause);
        this.algorithm = algorithm;
    }
}
