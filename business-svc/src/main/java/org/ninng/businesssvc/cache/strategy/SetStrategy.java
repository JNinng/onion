package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class SetStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.SET;
    }

    @Override
    public <V> Optional<V> get(RedissonClient rc, String key) {
        @SuppressWarnings("unchecked")
        Set<Object> data = rc.<Object>getSet(key).readAll();
        return data.isEmpty() ? Optional.empty() : Optional.of((V) data);
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        @SuppressWarnings("unchecked")
        Collection<Object> items = (Collection<Object>) value;
        RSet<Object> set = rc.getSet(key);
        set.delete();
        if (!items.isEmpty()) {
            set.addAll(items);
        }
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getSet(key).delete();
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (String keyStr : keys) {
            @SuppressWarnings("unchecked")
            V value = (V) get(rc, keyStr).orElse(null);
            if (value != null) {
                result.put(keyToId.apply(keyStr), value);
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
        RSet<V> set = rc.getSet(key.fullKey());
        set.delete();

        int page = 1;
        PageResult<V> pageResult;
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                set.addAll(pageResult.data());
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
