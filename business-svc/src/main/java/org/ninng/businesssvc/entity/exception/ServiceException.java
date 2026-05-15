package org.ninng.businesssvc.entity.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ninng.businesssvc.constant.HttpConstant;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -866771698179673030L;

    private int code;

    public ServiceException() {
        this.code = HttpConstant.ERROR;
    }

    public ServiceException(int code) {
        this.code = code;
    }

    public ServiceException(String message, int code) {
        super(message);
        this.code = code;
    }

    public ServiceException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public ServiceException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }

    public ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                            int code) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }
}
