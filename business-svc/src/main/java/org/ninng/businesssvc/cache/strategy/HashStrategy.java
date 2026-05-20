package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class HashStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.HASH;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(RedissonClient rc, String key) {
        RMap<String, Object> map = rc.getMap(key);
        Map<String, Object> all = map.readAllMap();
        return all.isEmpty() ? Optional.empty() : Optional.of((V) all);
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) value;
        rc.<String, Object>getMap(key).putAll(fields);
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getMap(key).delete();
    }

    public <V> Optional<V> getField(RedissonClient rc, String key, String field) {
        @SuppressWarnings("unchecked")
        V value = (V) rc.<String, Object>getMap(key).get(field);
        return Optional.ofNullable(value);
    }

    public <V> void putField(RedissonClient rc, String key, String field, V value) {
        rc.<String, V>getMap(key).fastPut(field, value);
    }

    public <V> Map<String, V> batchGetFields(RedissonClient rc, String key, Set<String> fields) {
        @SuppressWarnings("unchecked")
        Map<String, V> result = (Map<String, V>) rc.<String, Object>getMap(key).getAll(fields);
        return result;
    }

    public <V> void batchPutFields(RedissonClient rc, String key, Map<String, V> kvs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) kvs;
        rc.<String, Object>getMap(key).putAll(raw);
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            V value = (V) get(rc, key).orElse(null);
            if (value != null) {
                result.put(keyToId.apply(key), value);
            }
        }
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        kvMap.forEach((key, value) -> put(rc, key, value));
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                      CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                      int pageSize) {
        var idExtractor = domain.idExtractor();
        int page = 1;
        PageResult<V> pageResult;
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                for (V item : pageResult.data()) {
                    ID id = idExtractor != null ? idExtractor.apply(item) : null;
                    String fullKey = domain.buildKeyString(key.tid(), id);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fields = (Map<String, Object>) item;
                    rc.<String, Object>getMap(fullKey).putAll(fields);
                }
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        var keys = rc.getKeys();
        for (String k : keys.getKeysByPattern(pattern)) {
            keys.delete(k);
        }
    }
}
