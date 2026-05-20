package org.ninng.businesssvc.cache.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CacheDomains extends CacheDomain<Void, Void> {

    private CacheDomains() {
        super(null, null, null, new KeySpec(false, false, false),
              Void.class, Void.class, null, null);
    }

    public static final CacheDomain<Long, Long> USER =
            new CacheDomain<>("USER", CacheType.HASH, "onion:user:%s:%s",
                    KeySpec.TID_ID, Long.class, Long.class, null, null);

    public static final CacheDomain<Long, Long> TENANT =
            new CacheDomain<>("TENANT", CacheType.VALUE, "onion:tenant:%s",
                    KeySpec.TID, Long.class, Long.class, null, null);

    public static final CacheDomain<Long, Long> DEPT =
            new CacheDomain<>("DEPT", CacheType.LIST, "onion:dept:%s:list",
                    KeySpec.TID, Long.class, Long.class, null, null);

    public static final CacheDomain<String, Void> SYS_CONFIG =
            new CacheDomain<>("SYS_CONFIG", CacheType.VALUE, "onion:config:%s",
                    KeySpec.ID, String.class, Void.class, null, null);

    public static final CacheDomain<Void, Void> GLOBAL_LIST =
            new CacheDomain<>("GLOBAL_LIST", CacheType.LIST, "onion:global:list",
                    KeySpec.NONE, Void.class, Void.class, null, null);

    private static final Map<String, CacheDomain<?, ?>> REGISTRY = loadRegistry();

    private static Map<String, CacheDomain<?, ?>> loadRegistry() {
        Map<String, CacheDomain<?, ?>> map = new LinkedHashMap<>();
        try {
            for (Field field : CacheDomains.class.getFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) && Modifier.isPublic(mod)
                        && CacheDomain.class.isAssignableFrom(field.getType())) {
                    CacheDomain<?, ?> domain = (CacheDomain<?, ?>) field.get(null);
                    map.put(field.getName(), domain);
                }
            }
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        return Collections.unmodifiableMap(map);
    }

    public static CacheDomain<?, ?> resolve(String name) {
        CacheDomain<?, ?> domain = REGISTRY.get(name);
        if (domain == null) {
            throw new IllegalArgumentException("Unknown CacheDomain: " + name);
        }
        return domain;
    }

    public static Map<String, CacheDomain<?, ?>> all() {
        return REGISTRY;
    }
}
