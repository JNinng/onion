package org.ninng.businesssvc.config;

import org.ninng.businesssvc.cache.loader.CacheLoaderRegistry;
import org.ninng.businesssvc.cache.ops.CacheOps;
import org.ninng.businesssvc.cache.strategy.CacheStrategyFactory;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class BusinessCacheConfig {

    @Bean
    @ConditionalOnProperty(name = "onion.cache.business.local.enabled", havingValue = "false", matchIfMissing = true)
    public CacheOps cacheOps(RedissonClient redisson,
                             CacheStrategyFactory strategies,
                             CacheLoaderRegistry loaders) {
        return CacheOps.builder()
                .redisson(redisson)
                .strategies(strategies)
                .loaders(loaders)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "onion.cache.business.local.enabled", havingValue = "true")
    public CacheOps cacheOpsWithLocal(RedissonClient redisson,
                                       CacheStrategyFactory strategies,
                                       CacheLoaderRegistry loaders) {
        return CacheOps.builder()
                .redisson(redisson)
                .strategies(strategies)
                .loaders(loaders)
                .localCache(c -> c
                        .maximumSize(10000)
                        .expireAfterWrite(5, TimeUnit.MINUTES))
                .build();
    }
}
