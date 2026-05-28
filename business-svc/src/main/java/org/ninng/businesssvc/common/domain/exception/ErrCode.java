package org.ninng.businesssvc.common.domain.exception;

import lombok.Getter;

@Getter
public enum ErrCode {
    USER_NOT_FOUND("USER_NOT_FOUND");

    private final String code;

    ErrCode(String code) {
        this.code = code;
    }
}
