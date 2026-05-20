package org.ninng.businesssvc.cache.exception;

import org.ninng.businesssvc.cache.domain.CacheDomain;

public class CacheLoaderNotFoundException extends RuntimeException {
    public CacheLoaderNotFoundException(CacheDomain<?, ?> domain) {
        super("No CacheLoader registered for domain: " + domain.name());
    }
}
