package org.ninng.businesssvc.common.domain.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -9107226739965004008L;

    private String code;

    public BizException(String message, ErrCode errCode) {
        super(message);
        this.code = errCode.getCode();
    }
}
