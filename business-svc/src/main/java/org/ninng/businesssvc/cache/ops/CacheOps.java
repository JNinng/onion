package org.ninng.businesssvc.cache.ops;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.exception.CacheTypeMismatchException;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.CacheLoaderRegistry;
import org.ninng.businesssvc.cache.lock.CacheLockManager;
import org.ninng.businesssvc.cache.strategy.CacheStrategyFactory;
import org.ninng.businesssvc.cache.strategy.HashStrategy;
import org.ninng.businesssvc.cache.strategy.RefreshStrategy;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class CacheOps {

    private final RedissonClient redisson;
    private final CacheStrategyFactory strategies;
    private final CacheLoaderRegistry loaders;
    private final CacheLockManager locks;
    @Nullable
    private final Cache<Object, Object> localCache;
    private final RefreshStrategy defaultRefreshStrategy;

    private CacheOps(Builder builder) {
        this.redisson = builder.redisson;
        this.strategies = builder.strategies;
        this.loaders = builder.loaders;
        this.locks = new CacheLockManager(redisson);
        this.localCache = builder.localCache;
        this.defaultRefreshStrategy = builder.defaultRefreshStrategy;
    }

    // ═══════════════════════════════════════════
    //  单条操作
    // ═══════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public <ID, TID, V> Optional<V> get(CacheDomain<ID, TID> domain, TID tid, ID id) {
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        var strategy = strategies.get(domain.type());

        // L1: Caffeine
        if (localCache != null) {
            V cached = (V) localCache.getIfPresent(key.fullKey());
            if (cached != null) return Optional.of(cached);
        }

        // L2: Redisson
        Optional<V> result = strategy.get(redisson, key.fullKey());
        if (result.isPresent()) {
            if (localCache != null) localCache.put(key.fullKey(), result.get());
            return result;
        }

        // L3: Loader → 空结果自动 evict
        var loader = loaders.get(domain);
        V loaded = loader.load(key);
        if (loaded != null) {
            strategy.put(redisson, key.fullKey(), loaded);
            if (localCache != null) localCache.put(key.fullKey(), loaded);
            return Optional.of(loaded);
        }

        // null → 清理缓存
        strategy.evict(redisson, key.fullKey());
        if (localCache != null) localCache.invalidate(key.fullKey());
        return Optional.empty();
    }

    public <ID, TID, V> void put(CacheDomain<ID, TID> domain, TID tid, ID id, V value) {
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        var strategy = strategies.get(domain.type());
        strategy.put(redisson, key.fullKey(), value);
        if (localCache != null) localCache.put(key.fullKey(), value);
    }

    public <ID, TID> void evict(CacheDomain<ID, TID> domain, TID tid, ID id) {
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        strategies.get(domain.type()).evict(redisson, key.fullKey());
        if (localCache != null) localCache.invalidate(key.fullKey());
    }

    // ═══════════════════════════════════════════
    //  批量操作
    // ═══════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public <ID, TID, V> Map<ID, V> batchGet(CacheDomain<ID, TID> domain, TID tid, Set<ID> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        var strategy = strategies.get(domain.type());

        Map<String, ID> keyToId = new LinkedHashMap<>();
        for (ID id : ids) {
            keyToId.put(domain.buildKeyString(tid, id), id);
        }

        Map<ID, V> result = new LinkedHashMap<>();
        Set<String> missedKeys;

        // L1: Caffeine 过滤
        if (localCache != null) {
            missedKeys = new LinkedHashSet<>();
            keyToId.forEach((redisKey, id) -> {
                V cached = (V) localCache.getIfPresent(redisKey);
                if (cached != null) {
                    result.put(id, cached);
                } else {
                    missedKeys.add(redisKey);
                }
            });
        } else {
            missedKeys = new LinkedHashSet<>(keyToId.keySet());
        }

        if (missedKeys.isEmpty()) return result;

        // L2: Redisson 批量查
        Map<ID, V> fromRedis = strategy.batchGet(redisson, missedKeys, keyToId::get);
        fromRedis.forEach((id, v) -> {
            result.put(id, v);
            if (localCache != null) localCache.put(domain.buildKeyString(tid, id), v);
        });

        // L3: 仍未命中 → loader
        Set<ID> foundIds = new HashSet<>(result.keySet());
        Set<ID> stillMissing = new LinkedHashSet<>(ids);
        stillMissing.removeAll(foundIds);

        if (!stillMissing.isEmpty()) {
            var loader = loaders.get(domain);
            var patternKey = domain.buildKey(tid, null);
            Map<ID, V> loaded = loader.batchLoad(patternKey, stillMissing);

            Map<String, V> toPut = new LinkedHashMap<>();
            loaded.forEach((id, v) -> {
                String rk = domain.buildKeyString(tid, id);
                toPut.put(rk, v);
                result.put(id, v);
                stillMissing.remove(id);
            });
            if (!toPut.isEmpty()) {
                strategy.batchPut(redisson, toPut);
                if (localCache != null) toPut.forEach((rk, v) -> localCache.put(rk, v));
            }
            stillMissing.forEach(missedId ->
                    strategy.evict(redisson, domain.buildKeyString(tid, missedId)));
        }

        return result;
    }

    public <ID, TID, V> void batchPut(CacheDomain<ID, TID> domain, TID tid, Map<ID, V> data) {
        if (data.isEmpty()) return;
        Map<String, V> kvMap = new LinkedHashMap<>();
        data.forEach((id, v) -> kvMap.put(domain.buildKeyString(tid, id), v));
        strategies.get(domain.type()).batchPut(redisson, kvMap);
        if (localCache != null) kvMap.forEach(localCache::put);
    }

    // ═══════════════════════════════════════════
    //  全量刷新（加分布式锁）
    // ═══════════════════════════════════════════

    public <ID, TID> void refresh(CacheDomain<ID, TID> domain, TID tid) {
        refresh(domain, tid, defaultRefreshStrategy);
    }

    public <ID, TID> void refresh(CacheDomain<ID, TID> domain, TID tid, RefreshStrategy refreshStrategy) {
        locks.withLockOrThrow(domain.name() + ":" + tid, () -> {
            var key = domain.buildKey(tid, null);
            var strategy = strategies.get(domain.type());
            var loader = loaders.get(domain);

            strategy.clear(redisson, key.batchPattern());
            strategy.refresh(redisson, domain, loader, key, refreshStrategy.pageSize());
        });
    }

    // ═══════════════════════════════════════════
    //  Hash 扩展操作
    // ═══════════════════════════════════════════

    public <ID, TID, V> Optional<V> hget(CacheDomain<ID, TID> domain, TID tid, ID id, String field) {
        assertHash(domain);
        var key = domain.buildKey(tid, id);
        HashStrategy hash = strategies.hash();

        Optional<V> result = hash.getField(redisson, key.fullKey(), field);
        if (result.isPresent()) return result;

        var loader = loaders.get(domain);
        Object loaded = loader.load(key);
        if (loaded instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            V fieldVal = (V) m.get(field);
            if (fieldVal != null) {
                hash.putField(redisson, key.fullKey(), field, fieldVal);
                return Optional.of(fieldVal);
            }
        }
        return Optional.empty();
    }

    public <ID, TID, V> void hput(CacheDomain<ID, TID> domain, TID tid, ID id, String field, V value) {
        assertHash(domain);
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        strategies.hash().putField(redisson, key.fullKey(), field, value);
    }

    public <ID, TID, V> Map<String, V> hgetAll(CacheDomain<ID, TID> domain, TID tid, ID id, Set<String> fields) {
        assertHash(domain);
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        return strategies.hash().batchGetFields(redisson, key.fullKey(), fields);
    }

    public <ID, TID, V> void hputAll(CacheDomain<ID, TID> domain, TID tid, ID id, Map<String, V> kvs) {
        assertHash(domain);
        CacheKey<ID, TID> key = buildKey(domain, tid, id);
        strategies.hash().batchPutFields(redisson, key.fullKey(), kvs);
    }

    // ═══════════════════════════════════════════
    //  内部方法与 Builder
    // ═══════════════════════════════════════════

    /** 失效本地缓存（CacheEventBridge 用） */
    public <ID, TID> void invalidateLocal(CacheDomain<ID, TID> domain, TID tid, ID id) {
        if (localCache != null) {
            localCache.invalidate(domain.buildKeyString(tid, id));
        }
    }

    private <ID, TID> CacheKey<ID, TID> buildKey(CacheDomain<ID, TID> domain, TID tid, ID id) {
        return domain.buildKey(tid, id);
    }

    private void assertHash(CacheDomain<?, ?> domain) {
        if (domain.type() != CacheType.HASH) {
            throw new CacheTypeMismatchException(CacheType.HASH, domain.type());
        }
    }

    // ─── Builder ───

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RedissonClient redisson;
        private CacheStrategyFactory strategies;
        private CacheLoaderRegistry loaders;
        @Nullable
        private Cache<Object, Object> localCache;
        private RefreshStrategy defaultRefreshStrategy = RefreshStrategy.defaultStrategy();

        public Builder redisson(RedissonClient rc) {
            this.redisson = rc;
            return this;
        }

        public Builder strategies(CacheStrategyFactory sf) {
            this.strategies = sf;
            return this;
        }

        public Builder loaders(CacheLoaderRegistry lr) {
            this.loaders = lr;
            return this;
        }

        public Builder localCache(Consumer<Caffeine<Object, Object>> configurer) {
            Caffeine<Object, Object> cb = Caffeine.newBuilder();
            configurer.accept(cb);
            this.localCache = cb.build();
            return this;
        }

        public Builder defaultRefreshStrategy(RefreshStrategy strategy) {
            this.defaultRefreshStrategy = strategy;
            return this;
        }

        public CacheOps build() {
            Objects.requireNonNull(redisson, "RedissonClient must not be null");
            Objects.requireNonNull(strategies, "CacheStrategyFactory must not be null");
            Objects.requireNonNull(loaders, "CacheLoaderRegistry must not be null");
            return new CacheOps(this);
        }
    }
}
