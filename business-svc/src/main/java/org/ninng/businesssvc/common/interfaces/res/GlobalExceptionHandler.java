package org.ninng.businesssvc.common.interfaces.res;

import com.fasterxml.jackson.core.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.entity.exception.BizException;
import org.ninng.businesssvc.entity.exception.ErrCode;
import org.ninng.businesssvc.entity.exception.PermissionsException;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final I18nUtil i18n;

    public GlobalExceptionHandler(I18nUtil i18n) {
        this.i18n = i18n;
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAllExceptions(Exception ex) {
        log.error(ex.getMessage(), ex);
        if (ex.getCause() instanceof JsonParseException) {
            return R.err(i18n.getMessage("exception.jsonParseErr"), ErrCode.JSON_PARSER_ERR);
        }
        return R.err(i18n.getMessage("exception.unknownErr"), ErrCode.UNKNOW_EXCEPTION);
    }

    @ExceptionHandler(ServiceException.class)
    public R<?> handleServiceException(ServiceException ex) {
        log.error(ex.getMessage(), ex);
        return new R<>(ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(PermissionsException.class)
    public R<?> handlePermissionsException(PermissionsException ex) {
        log.error(ex.getMessage(), ex);
        return new R<>(ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(org.ninng.businesssvc.entity.exception.SecurityException.class)
    public R<?> handleSecurityException(org.ninng.businesssvc.entity.exception.SecurityException ex) {
        log.error(ex.getMessage(), ex);
        R<Void> r = new R<>();
        r.setCode(ErrCode.SECURITY_EXCEPTION.getCode());
        r.setAlgorithm(ex.getAlgorithm());
        r.setMsg(ex.getMessage());
        return r;
    }

    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException ex) {
        log.error(ex.getMessage(), ex);
        return new R<>(ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(SQLException.class)
    public R<?> handleSQLException(SQLException ex) {
        log.error(ex.getMessage(), ex);
        return R.err(ex.getMessage(), ErrCode.DB_EXCEPTION);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error(ex.getMessage(), ex);
        String message = Objects.requireNonNull(ex.getBindingResult()
                        .getFieldError())
                .getDefaultMessage();
        String field = Objects.requireNonNull(ex.getBindingResult()
                        .getFieldError())
                .getField();
        return R.err(field + ": " + message, ErrCode.PARAMETER_EXCEPTION);
    }
}