package org.ninng.businesssvc.cache.exception;

public class CacheLockException extends RuntimeException {
    public CacheLockException(String message) {
        super(message);
    }
}
