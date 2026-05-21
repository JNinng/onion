package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface CacheTypeStrategy {

    CacheType type();

    <V> Optional<V> get(RedissonClient rc, String key);

    <V> void put(RedissonClient rc, String key, V value);

    void evict(RedissonClient rc, String key);

    <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                Function<String, ID> keyToId);

    <V> void batchPut(RedissonClient rc, Map<String, V> kvMap);

    <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                              CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                              int pageSize);

    void clear(RedissonClient rc, String pattern);
}
