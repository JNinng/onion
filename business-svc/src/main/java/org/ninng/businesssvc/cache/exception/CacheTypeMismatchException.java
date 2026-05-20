package org.ninng.businesssvc.cache.exception;

import org.ninng.businesssvc.cache.domain.CacheType;

public class CacheTypeMismatchException extends RuntimeException {
    public CacheTypeMismatchException(CacheType expected, CacheType actual) {
        super("Expected " + expected + " but got " + actual);
    }
}
