package org.ninng.businesssvc.entity.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -9107226739965004008L;

    private ErrCode code;

    public BizException(String message, ErrCode errCode) {
        super(message);
        this.code = errCode;
    }

    public BizException(String message, ErrCode errCode, Throwable cause) {
        super(message, cause);
        this.code = errCode;
    }
}
