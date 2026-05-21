package org.ninng.businesssvc.cache.bridge;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.ops.CacheOps;
import org.ninng.businesssvc.event.CacheInvalidateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CacheEventBridge {

    private static final Logger log = LoggerFactory.getLogger(CacheEventBridge.class);
    private static final Map<String, CacheDomain<?, ?>> LEGACY_MAP = Map.of();
    private final CacheOps cacheOps;

    public CacheEventBridge(CacheOps cacheOps) {
        this.cacheOps = cacheOps;
    }

    @EventListener
    public void onLegacyCacheInvalidate(CacheInvalidateEvent event) {
        event.evictions()
                .forEach((cacheName, keys) -> {
                    CacheDomain<?, ?> domain = LEGACY_MAP.get(cacheName);
                    if (domain == null) {
                        return;
                    }

                    if (keys == null || keys.isEmpty()) {
                        log.debug("Legacy cache '{}' cleared, no keys to invalidate", cacheName);
                        return;
                    }

                    for (Object key : keys) {
                        if (key instanceof String k) {
                            invalidateLocalKey(domain, k);
                        }
                    }
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invalidateLocalKey(CacheDomain<?, ?> domain, String key) {
        try {
            cacheOps.invalidateLocal((CacheDomain) domain, null, key);
            log.debug("Legacy cache invalidation: domain={} key={}", domain.name(), key);
        } catch (Exception e) {
            log.debug("Cannot invalidate legacy key for domain {}: {}", domain.name(), e.getMessage());
        }
    }
}
