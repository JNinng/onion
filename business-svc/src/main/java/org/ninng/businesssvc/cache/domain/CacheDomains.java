package org.ninng.businesssvc.cache.domain;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CacheDomains {

    private final Map<String, CacheDomain<?, ?>> registry;

    public CacheDomains(List<CacheDomain<?, ?>> domains) {
        Map<String, CacheDomain<?, ?>> map = new LinkedHashMap<>();
        for (CacheDomain<?, ?> domain : domains) {
            CacheDomain<?, ?> existing = map.put(domain.name(), domain);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate CacheDomain: " + domain.name() +
                                " — already registered by " + existing);
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    public CacheDomain<?, ?> resolve(String name) {
        CacheDomain<?, ?> domain = registry.get(name);
        if (domain == null) {
            throw new IllegalArgumentException("Unknown CacheDomain: " + name);
        }
        return domain;
    }

    public Map<String, CacheDomain<?, ?>> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registry));
    }
}
