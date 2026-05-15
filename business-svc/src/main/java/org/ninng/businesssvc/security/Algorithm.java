package org.ninng.businesssvc.security;

import lombok.Getter;

@Getter
public enum Algorithm {
    NONE("NONE"),
    AES("AES"),
    ;
    private final String value;

    Algorithm(String value) {
        this.value = value;
    }
}
