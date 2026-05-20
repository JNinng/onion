package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.domain.CacheKey;
import org.springframework.lang.Nullable;

import java.util.*;

public interface CacheLoader<ID, TID, V> {

    @Nullable
    V load(CacheKey<ID, TID> key);

    PageResult<V> loadPage(CacheKey<ID, TID> key, int page, int pageSize);

    default Map<ID, V> batchLoad(CacheKey<ID, TID> pattern, Set<ID> ids) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (ID id : ids) {
            CacheKey<ID, TID> singleKey = pattern.domain().buildKey(pattern.tid(), id);
            V value = load(singleKey);
            if (value != null) {
                result.put(id, value);
            }
        }
        return result;
    }
}
