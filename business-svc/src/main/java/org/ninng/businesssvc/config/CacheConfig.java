package org.ninng.businesssvc.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.ninng.businesssvc.constant.CacheConstant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    private final Set<String> NAMES = Set.of(CacheConstant.USER, CacheConstant.USER_ROLE);

    @Bean("redisObjectMapper")
    public ObjectMapper getRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new ImmutableModule());
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置Caffeine缓存策略
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(10000)          // 初始容量
                .maximumSize(100000)             // 最大条目数
                .expireAfterWrite(20, TimeUnit.MINUTES) // 写入后20分钟过期
                .recordStats());               // 记录统计信息

        // 可选：指定要管理的缓存名称
        cacheManager.setCacheNames(NAMES);
        return cacheManager;
    }

    @Bean
    public RedisCacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory,
                                          @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
                .computePrefixWith(cacheName -> CacheConstant.KEY_PREFIX + ":" + cacheName + ":")
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper, JacksonObjectReader.create(),
                                JacksonObjectWriter.create())))
                .disableCachingNullValues();
        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(lettuceConnectionFactory)
                .cacheDefaults(configuration)
                .transactionAware()
                .initialCacheNames(NAMES)
                .build();
    }

    /**
     * 主缓存管理器
     */
    @Primary
    @Bean
    public CompositeCacheManager compositeCacheManager(CaffeineCacheManager caffeineCacheManager,
                                                       RedisCacheManager redisCacheManager) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();

        // 设置缓存管理器列表（先本地后远程）
        compositeCacheManager.setCacheManagers(List.of(caffeineCacheManager, redisCacheManager));

        // 设置为true：当所有CacheManager都没有找到缓存时返回null
        // 设置为false：至少有一个CacheManager能处理缓存操作
        compositeCacheManager.setFallbackToNoOpCache(false);

        return compositeCacheManager;
    }
}
