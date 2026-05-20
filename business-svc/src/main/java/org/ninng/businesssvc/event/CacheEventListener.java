package org.ninng.businesssvc.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ninng.businesssvc.constant.CacheConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class CacheEventListener {

    private static final Logger log = LoggerFactory.getLogger(CacheEventListener.class);
    private final CacheManager cacheManager;
    private final String instanceId = java.util.UUID.randomUUID()
            .toString();

    @Autowired(required = false)
    @Qualifier("redisObjectMapper")
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public CacheEventListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener
    public void onCacheInvalidate(CacheInvalidateEvent event) {
        var evictions = event.evictions();
        if (evictions.isEmpty()) {
            return;
        }
        if (event instanceof CacheInvalidateEvent.Local) {
            log.debug("Local cache invalidation: {}", evictions.keySet());
            publishToRedis(evictions);
        } else if (event instanceof CacheInvalidateEvent.Redis redis) {
            if (instanceId.equals(redis.sourceInstanceId())) {
                log.debug("Ignoring self-published Redis invalidation");
                return;
            }
            log.debug("Redis cache invalidation: {}", evictions.keySet());
        }
        evictions.forEach(this::handleEviction);
    }

    private void handleEviction(String cacheName, Set<Object> keys) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' not found, skipping eviction", cacheName);
            return;
        }
        if (keys == null || keys.isEmpty()) {
            cache.clear();
            log.debug("Cleared cache '{}'", cacheName);
        } else {
            keys.forEach(key -> {
                cache.evict(key);
                log.debug("Evicted cache '{}' key '{}'", cacheName, key);
            });
        }
    }

    private void publishToRedis(Map<String, Set<Object>> evictions) {
        if (objectMapper == null || redisTemplate == null) {
            return;
        }
        try {
            var msg = objectMapper.writeValueAsString(new CacheInvalidateMessage(instanceId, evictions));
            redisTemplate.convertAndSend(CacheConstant.INVALIDATION_CHANNEL, msg);
            log.debug("Published cache invalidation to Redis channel '{}'", CacheConstant.INVALIDATION_CHANNEL);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation to Redis", e);
        }
    }

    record CacheInvalidateMessage(String sourceInstanceId, Map<String, Set<Object>> evictions) {
    }
}
