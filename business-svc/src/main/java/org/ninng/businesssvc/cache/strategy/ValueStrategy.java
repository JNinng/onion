package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RBucket;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Component
public class ValueStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.VALUE;
    }

    @Override
    public <V> Optional<V> get(RedissonClient rc, String key) {
        RBucket<V> bucket = rc.getBucket(key);
        return Optional.ofNullable(bucket.get());
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        rc.<V>getBucket(key)
                .set(value);
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getBucket(key)
                .delete();
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                       Function<String, ID> keyToId) {
        var batch = rc.createBatch();
        Map<String, RFuture<V>> futures = new LinkedHashMap<>();
        for (String key : keys) {
            futures.put(key, batch.<V>getBucket(key)
                    .getAsync());
        }
        batch.execute();

        Map<ID, V> result = new LinkedHashMap<>();
        futures.forEach((key, future) -> {
            V value = future.toCompletableFuture()
                    .getNow(null);
            if (value != null) {
                result.put(keyToId.apply(key), value);
            }
        });
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        var batch = rc.createBatch();
        kvMap.forEach((key, value) -> batch.getBucket(key)
                .setAsync(value));
        batch.execute();
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                     CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                     int pageSize) {
        int page = 1;
        PageResult<V> pageResult;
        var idExtractor = domain.idExtractor();
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                Map<String, V> kvMap = new LinkedHashMap<>();
                for (V item : pageResult.data()) {
                    ID id = idExtractor != null ? idExtractor.apply(item) : null;
                    String fullKey = domain.buildKeyString(key.tid(), id);
                    kvMap.put(fullKey, item);
                }
                batchPut(rc, kvMap);
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        var keys = rc.getKeys();
        for (String key : keys.getKeys(KeysScanOptions.defaults()
                .pattern(pattern))) {
            keys.delete(key);
        }
    }
}
