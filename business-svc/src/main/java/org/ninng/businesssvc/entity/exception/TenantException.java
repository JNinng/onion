package org.ninng.businesssvc.entity.exception;

import lombok.Getter;
import lombok.Setter;
import org.ninng.businesssvc.context.UserContextHolder;

import java.io.Serial;

@Getter
@Setter
public class TenantException extends BizException {

    @Serial
    private static final long serialVersionUID = -756258420812033135L;

    private String tenantId;

    public TenantException(String message, ErrCode errCode) {
        super(message, errCode);
        this.tenantId = UserContextHolder.getTenantId();
    }

    public TenantException(String message, String tenantId, ErrCode errCode) {
        super(message, errCode);
        this.tenantId = tenantId;
    }
}
