package org.ninng.businesssvc.entity.exception;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.LinkContextHolder;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.R;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<String> SENSITIVE_PARAMS = Set.of("password", "secret", "token", "authorization", "sign",
            "hmac", "oldPassword", "newPassword", "confirmPassword");

    private static final int MAX_BODY_LOG_LENGTH = 2000;

    private final I18nUtil i18n;

    public GlobalExceptionHandler(I18nUtil i18n) {
        this.i18n = i18n;
    }

    private static String ctxPrefix(HttpServletRequest request) {
        return "traceId=" + LinkContextHolder.getTraceId() + ", uri=" + request.getRequestURI() + ", method=" + request.getMethod() + ", tenantId=" + UserContextHolder.getTenantId() + ", userId=" + UserContextHolder.getUserId();
    }

    private static String params(Map<String, String[]> map) {
        if (map.isEmpty()) {
            return "params=";
        }
        return map.entrySet()
                .stream()
                .map(e -> {
                    String key = e.getKey();
                    String value = SENSITIVE_PARAMS.contains(key) ? "***" : String.join(",", e.getValue());
                    return key + "=" + value;
                })
                .collect(Collectors.joining("&", "params=", ""));
    }

    private static String body(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return "body=";
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content.length == 0) {
            return "body=";
        }
        String str = new String(content, StandardCharsets.UTF_8).replace('\r', ' ')
                .replace('\n', ' ');
        if (str.length() > MAX_BODY_LOG_LENGTH) {
            str = str.substring(0, MAX_BODY_LOG_LENGTH) + "...";
        }
        return "body=" + str;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.error("PARAM: {}, {}, {}, param={}, type={}", ctxPrefix(request), params(request.getParameterMap()),
                body(request), ex.getParameterName(), ex.getParameterType(), ex);
        return R.err(ex.getMessage(), ErrCode.PARAMETER_EXCEPTION);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fieldError = Objects.requireNonNull(ex.getBindingResult()
                .getFieldError());
        log.error("PARAM: {}, {}, {}, field={}, rejected={}, msg={}", ctxPrefix(request),
                params(request.getParameterMap()), body(request), fieldError.getField(), fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(), ex);
        return R.err(fieldError.getField() + ": " + fieldError.getDefaultMessage(), ErrCode.PARAMETER_EXCEPTION);
    }

    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException ex, HttpServletRequest request) {
        log.error("BIZ: {}, {}, {}, code={}", ctxPrefix(request), params(request.getParameterMap()), body(request),
                ex.getCode(), ex);
        return R.err(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ServiceException.class)
    public R<?> handleServiceException(ServiceException ex, HttpServletRequest request) {
        log.error("SERVICE: {}, {}, {}, code={}", ctxPrefix(request), params(request.getParameterMap()), body(request),
                ex.getCode(), ex);
        return R.err(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(PermissionsException.class)
    public R<?> handlePermissionsException(PermissionsException ex, HttpServletRequest request) {
        log.error("PERMISSION: {}, {}, {}, code={}", ctxPrefix(request), params(request.getParameterMap()),
                body(request), ex.getCode(), ex);
        return R.err(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(org.ninng.businesssvc.entity.exception.SecurityException.class)
    public R<?> handleSecurityException(org.ninng.businesssvc.entity.exception.SecurityException ex,
                                        HttpServletRequest request) {
        log.error("SECURITY: {}, {}, {}, code={}, algorithm={}", ctxPrefix(request), params(request.getParameterMap()),
                body(request), ErrCode.SECURITY_EXCEPTION, ex.getAlgorithm(), ex);
        return R.err(ex.getMessage(), ErrCode.SECURITY_EXCEPTION, ex.getAlgorithm());
    }

    @ExceptionHandler(SQLException.class)
    public R<?> handleSQLException(SQLException ex, HttpServletRequest request) {
        log.error("DB: {}, {}, {}, sqlState={}, errorCode={}", ctxPrefix(request), params(request.getParameterMap()),
                body(request), ex.getSQLState(), ex.getErrorCode(), ex);
        return R.err(i18n.getMessage("exception.dbErr"), ErrCode.DB_EXCEPTION);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("UNKNOWN: {}, {}, {}", ctxPrefix(request), params(request.getParameterMap()), body(request), ex);
        if (ex.getCause() instanceof JsonParseException) {
            return R.err(i18n.getMessage("exception.jsonParseErr"), ErrCode.JSON_PARSER_ERR);
        }
        return R.err(i18n.getMessage("exception.unknownErr"), ErrCode.UNKNOW_EXCEPTION);
    }
}
