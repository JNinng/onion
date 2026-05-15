package org.ninng.businesssvc.entity.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class SecurityException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 342686615400929388L;

    private String algorithm;

    public SecurityException(String algorithm) {
        this.algorithm = algorithm;
    }

    public SecurityException(String message, String algorithm) {
        super(message);
        this.algorithm = algorithm;
    }

    public SecurityException(String message, Throwable cause, String algorithm) {
        super(message, cause);
        this.algorithm = algorithm;
    }

    public SecurityException(Throwable cause, String algorithm) {
        super(cause);
        this.algorithm = algorithm;
    }

    public SecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                             String algorithm) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.algorithm = algorithm;
    }
}
