package org.ninng.businesssvc.cache.bridge;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheDomains;
import org.ninng.businesssvc.cache.ops.CacheOps;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.event.CacheInvalidateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class CacheEventBridge {

    private static final Logger log = LoggerFactory.getLogger(CacheEventBridge.class);

    private final CacheOps cacheOps;

    private static final Map<String, CacheDomain<?, ?>> LEGACY_MAP = Map.of(
            CacheConstant.USER, CacheDomains.USER
    );

    public CacheEventBridge(CacheOps cacheOps) {
        this.cacheOps = cacheOps;
    }

    @EventListener
    public void onLegacyCacheInvalidate(CacheInvalidateEvent event) {
        event.evictions().forEach((cacheName, keys) -> {
            CacheDomain<?, ?> domain = LEGACY_MAP.get(cacheName);
            if (domain == null) return;

            if (keys == null || keys.isEmpty()) {
                log.debug("Legacy cache '{}' cleared, no specific keys to invalidate", cacheName);
                return;
            }

            for (Object key : keys) {
                if (key instanceof String k) {
                    log.debug("Legacy event for {} key={}, invalidating local cache", cacheName, key);
                }
            }
        });
    }
}
