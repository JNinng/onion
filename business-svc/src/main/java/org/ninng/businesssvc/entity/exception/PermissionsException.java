package org.ninng.businesssvc.entity.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 数据权限异常
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PermissionsException extends BizException {

    @Serial
    private static final long serialVersionUID = 6656335940441684110L;

    public PermissionsException(String message) {
        super(message, ErrCode.NOT_PERMISSION);
    }
}
