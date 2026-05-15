package org.ninng.businesssvc.handler;

import com.fasterxml.jackson.core.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.ninng.businesssvc.constant.HttpConstant;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLException;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public R<?> handleAllExceptions(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        if (ex.getCause() instanceof JsonParseException) {
            return R.err("JSON 异常");
        }
        return R.err("未知异常");
    }

    @ExceptionHandler(ServiceException.class)
    public R<?> handleServiceException(ServiceException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        return new R<>(ex.getCode(), ex.getMessage(), null, null);
    }

    @ExceptionHandler(org.ninng.businesssvc.entity.exception.SecurityException.class)
    public R<?> handleSecurityException(org.ninng.businesssvc.entity.exception.SecurityException ex,
                                        WebRequest request) {
        log.error(ex.getMessage(), ex);
        R<Void> r = new R<>();
        r.setCode(HttpConstant.ERROR);
        r.setAlgorithm(ex.getAlgorithm());
        r.setMsg(ex.getMessage());
        return r;
    }

    @ExceptionHandler(SQLException.class)
    public R<?> handleSQLException(SQLException ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        return R.err(ex.getMessage());
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
        return R.err(field + ": " + message);
    }
}
