package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheDomains;
import org.ninng.businesssvc.cache.exception.CacheLoaderNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CacheLoaderRegistry {

    private final Map<CacheDomain<?, ?>, CacheLoader<?, ?, ?>> registry;

    public CacheLoaderRegistry(List<CacheLoader<?, ?, ?>> loaders) {
        Map<CacheDomain<?, ?>, CacheLoader<?, ?, ?>> map = new LinkedHashMap<>();
        for (CacheLoader<?, ?, ?> loader : loaders) {
            ForCache anno = loader.getClass().getAnnotation(ForCache.class);
            if (anno == null) continue;
            CacheDomain<?, ?> domain = CacheDomains.resolve(anno.value());
            CacheLoader<?, ?, ?> existing = map.put(domain, loader);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate CacheLoader for domain " + domain.name() +
                        ": " + existing.getClass().getName() + " and " + loader.getClass().getName());
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public <ID, TID, V> CacheLoader<ID, TID, V> get(CacheDomain<ID, TID> domain) {
        CacheLoader<?, ?, ?> loader = registry.get(domain);
        if (loader == null) {
            throw new CacheLoaderNotFoundException(domain);
        }
        return (CacheLoader<ID, TID, V>) loader;
    }

    public boolean contains(CacheDomain<?, ?> domain) {
        return registry.containsKey(domain);
    }

    public Set<CacheDomain<?, ?>> registeredDomains() {
        return registry.keySet();
    }
}
