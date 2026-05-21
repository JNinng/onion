package org.ninng.businesssvc.cache;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.domain.KeySpec;
import org.ninng.businesssvc.entity.GlobalListItem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheTestConfig {

    /**
     * 域名常量，供 {@link org.ninng.businesssvc.cache.loader.CacheLoader#name()} 引用，形成编译期硬关联
     */
    public static final String GLOBAL_TEST = "GLOBAL_TEST";

    public static final CacheDomain<String, Void> GLOBAL_TEST_DOMAIN = new CacheDomain<>(
            GLOBAL_TEST, CacheType.VALUE, "onion:global:test:%s",
            KeySpec.ID, String.class, Void.class, null, null)
            .withIdExtractor(obj -> ((GlobalListItem) obj).getId())
            .withValueType(GlobalListItem.class);

    @Bean
    CacheDomain<?, ?> globalTestDomain() {
        return GLOBAL_TEST_DOMAIN;
    }
}
