package org.ninng.businesssvc.entity.exception;

import lombok.Getter;

@Getter
public enum ErrCode {
    OK("OK"),
    UNKNOW_EXCEPTION("UNKNOW_EXCEPTION"),
    USER_NOT_FOUND("USER_NOT_FOUND"),
    NOT_PERMISSION("NOT_PERMISSION"),
    SERVICE_EXCEPTION("SERVICE_EXCEPTION"),
    DB_EXCEPTION("DB_EXCEPTION"),
    SECURITY_EXCEPTION("SECURITY_EXCEPTION"),
    JSON_PARSER_ERR("JSON_PARSER_ERR"),
    PARAMETER_EXCEPTION("PARAMETER_EXCEPTION");

    private final String code;

    ErrCode(String code) {
        this.code = code;
    }
}
