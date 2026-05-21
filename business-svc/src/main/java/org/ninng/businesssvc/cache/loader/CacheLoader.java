package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.domain.CacheKey;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface CacheLoader<ID, TID, V> {

    /**
     * 归属的 {@link org.ninng.businesssvc.cache.domain.CacheDomain} 名称，通过 Spring 容器的 {@code CacheDomain} Bean 自动注册
     */
    String name();

    @Nullable
    V load(CacheKey<ID, TID> key);

    PageResult<V> loadPage(CacheKey<ID, TID> key, int page, int pageSize);

    default Map<ID, V> batchLoad(CacheKey<ID, TID> pattern, Set<ID> ids) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (ID id : ids) {
            CacheKey<ID, TID> singleKey = pattern.domain()
                    .buildKey(pattern.tid(), id);
            V value = load(singleKey);
            if (value != null) {
                result.put(id, value);
            }
        }
        return result;
    }
}
