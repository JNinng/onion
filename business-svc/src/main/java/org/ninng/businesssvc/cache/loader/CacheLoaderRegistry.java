package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheDomains;
import org.ninng.businesssvc.cache.exception.CacheLoaderNotFoundException;
import org.ninng.businesssvc.component.I18nUtil;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CacheLoaderRegistry {

    private final Map<CacheDomain<?, ?>, CacheLoader<?, ?, ?>> registry;

    public CacheLoaderRegistry(List<CacheLoader<?, ?, ?>> loaders, CacheDomains cacheDomains, I18nUtil i18nUtil) {
        Map<CacheDomain<?, ?>, CacheLoader<?, ?, ?>> map = new LinkedHashMap<>();
        for (CacheLoader<?, ?, ?> loader : loaders) {
            String domainName = loader.name();
            if (domainName == null || domainName.isBlank()) {
                continue;
            }
            CacheDomain<?, ?> domain = cacheDomains.resolve(domainName);
            CacheLoader<?, ?, ?> existing = map.put(domain, loader);
            if (existing != null) {
                throw new IllegalStateException(
                        i18nUtil.getMessage("exception.duplicateCacheLoader", new Object[]{domain.name()}));
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
