package org.ninng.businesssvc.cache.domain;

import org.ninng.businesssvc.component.I18nUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CacheDomains {

    private final Map<String, CacheDomain<?, ?>> registry;
    private final I18nUtil i18nUtil;

    public CacheDomains(List<CacheDomain<?, ?>> domains, I18nUtil i18nUtil) {
        this.i18nUtil = i18nUtil;
        Map<String, CacheDomain<?, ?>> map = new LinkedHashMap<>();
        for (CacheDomain<?, ?> domain : domains) {
            CacheDomain<?, ?> existing = map.put(domain.name(), domain);
            if (existing != null) {
                throw new IllegalStateException(
                        i18nUtil.getMessage("exception.duplicateCacheDomain", new Object[]{domain.name()}));
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    public CacheDomain<?, ?> resolve(String name) {
        CacheDomain<?, ?> domain = registry.get(name);
        if (domain == null) {
            throw new IllegalArgumentException(i18nUtil.getMessage("exception.unknownCacheDomain", new Object[]{name}));
        }
        return domain;
    }

    public Map<String, CacheDomain<?, ?>> all() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registry));
    }
}
