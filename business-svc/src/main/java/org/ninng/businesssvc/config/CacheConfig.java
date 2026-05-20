package org.ninng.businesssvc.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.ninng.businesssvc.constant.CacheConstant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
                .computePrefixWith(cacheName -> CacheConstant.KEY_PREFIX + ":auto:" + cacheName + ":")
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
    public CaffeineRedisCacheManager compositeCacheManager(CaffeineCacheManager caffeineCacheManager,
                                                           RedisCacheManager redisCacheManager) {
        return new CaffeineRedisCacheManager(caffeineCacheManager, redisCacheManager);
    }

    public static class CaffeineRedisCache implements Cache {

        private final Cache caffeineCache;
        private final Cache redisCache;
        private final String name;

        public CaffeineRedisCache(String name, Cache caffeineCache, Cache redisCache) {
            this.name = name;
            this.caffeineCache = caffeineCache;
            this.redisCache = redisCache;
        }

        @Override
        @NonNull
        public String getName() {
            return this.name;
        }

        @Override
        @NonNull
        public Object getNativeCache() {
            // 返回底层 Caffeine 的原生 Cache 对象
            return caffeineCache.getNativeCache();
        }

        @Override
        @Nullable
        public ValueWrapper get(@NonNull Object key) {
            // 1. 先查 Caffeine (L1)
            ValueWrapper wrapper = caffeineCache.get(key);
            if (wrapper != null) {
                return wrapper;
            }

            // 2. Caffeine 未命中，查 Redis (L2)
            wrapper = redisCache.get(key);
            if (wrapper != null) {
                // 3. Redis 命中，回填到 Caffeine (L1)
                caffeineCache.put(key, wrapper.get());
            }
            return wrapper;
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key, @NonNull Class<T> type) {
            ValueWrapper wrapper = get(key);
            if (wrapper != null) {
                Object value = wrapper.get();
                if (value != null && !type.isInstance(value)) {
                    throw new IllegalStateException(
                            "Cached value is not of required type [" + type.getName() + "]: " + value);
                }
                return (T) value;
            }
            return null;
        }

        @Override
        @Nullable
        public <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
            // 1. 先查 Caffeine
            ValueWrapper wrapper = caffeineCache.get(key);
            if (wrapper != null) {
                return (T) wrapper.get();
            }

            // 2. 查 Redis
            wrapper = redisCache.get(key);
            if (wrapper != null) {
                caffeineCache.put(key, wrapper.get());
                return (T) wrapper.get();
            }

            // 3. 都未命中，执行 valueLoader 加载
            T value;
            try {
                value = valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }

            // 4. 加载到的数据同步到 L1 和 L2
            put(key, value);
            return value;
        }

        @Override
        public void put(@NonNull Object key, @Nullable Object value) {
            // 同步写入 L1 和 L2
            redisCache.put(key, value);
            caffeineCache.put(key, value);
        }

        @Override
        public void evict(@NonNull Object key) {
            // 同步删除 L1 和 L2 (先删 Redis，再删 Caffeine，防止删 Redis 瞬间 Caffeine 又读到了旧数据)
            redisCache.evict(key);
            caffeineCache.evict(key);
        }

        @Override
        public boolean evictIfPresent(@NonNull Object key) {
            // Spring 5.3+ 提供，主要用于 Caffeine 判断是否真实移除
            boolean redisEvicted = redisCache.evictIfPresent(key);
            boolean caffeineEvicted = caffeineCache.evictIfPresent(key);
            return redisEvicted || caffeineEvicted;
        }

        @Override
        public void clear() {
            // 同步清空 L1 和 L2
            redisCache.clear();
            caffeineCache.clear();
        }

        @Override
        public boolean invalidate() {
            // Spring 5.3+ 提供
            boolean redisInvalidated = redisCache.invalidate();
            boolean caffeineInvalidated = caffeineCache.invalidate();
            return redisInvalidated || caffeineInvalidated;
        }
    }

    public static class CaffeineRedisCacheManager implements CacheManager {

        private final CacheManager caffeineCacheManager;
        private final CacheManager redisCacheManager;
        private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

        public CaffeineRedisCacheManager(CacheManager caffeineCacheManager, CacheManager redisCacheManager) {
            this.caffeineCacheManager = caffeineCacheManager;
            this.redisCacheManager = redisCacheManager;
        }

        @Override
        @NonNull
        public Cache getCache(@NonNull String name) {
            return cacheMap.computeIfAbsent(name, key -> {
                Cache caffeineCache = caffeineCacheManager.getCache(key);
                Cache redisCache = redisCacheManager.getCache(key);
                if (caffeineCache == null || redisCache == null) {
                    throw new IllegalArgumentException(
                            "Cannot find cache named '" + key + "' in one of the cache managers");
                }
                return new CaffeineRedisCache(key, caffeineCache, redisCache);
            });
        }

        @Override
        @NonNull
        public Collection<String> getCacheNames() {
            Set<String> cacheNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
            cacheNames.addAll(caffeineCacheManager.getCacheNames());
            cacheNames.addAll(redisCacheManager.getCacheNames());
            return Collections.unmodifiableSet(cacheNames);
        }
    }
}
