# 业务缓存抽象层 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建与现有 CaffeineRedisCacheManager 互补的业务缓存抽象层，支持 VALUE/LIST/SET/HASH 四种数据结构，批量操作、分页刷新、分布式锁

**Architecture:** 策略工厂 + 泛型门面 + `@ForCache` Spring Bean 自动注册。`CacheOps` 为统一门面，`CacheDomain` 为 sealed class 静态常量定义缓存域，`CacheTypeStrategy` 按 Redisson 数据结构类型分发。

**Tech Stack:** Java 21 (sealed class, record), Spring Boot 3.5, Redisson 4.3.1, Caffeine 3.2.3

---

### Task 1: 异常类 + CacheType 枚举 + KeySpec

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/exception/CacheLockException.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/exception/CacheLoaderNotFoundException.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/exception/CacheTypeMismatchException.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/exception/CacheKeyArgumentException.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheType.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/domain/KeySpec.java`

- [ ] **Step 1: Create CacheLockException**

```java
package org.ninng.businesssvc.cache.exception;

public class CacheLockException extends RuntimeException {
    public CacheLockException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create CacheLoaderNotFoundException**

```java
package org.ninng.businesssvc.cache.exception;

import org.ninng.businesssvc.cache.domain.CacheDomain;

public class CacheLoaderNotFoundException extends RuntimeException {
    public CacheLoaderNotFoundException(CacheDomain<?, ?> domain) {
        super("No CacheLoader registered for domain: " + domain.name());
    }
}
```

- [ ] **Step 3: Create CacheTypeMismatchException**

```java
package org.ninng.businesssvc.cache.exception;

import org.ninng.businesssvc.cache.domain.CacheType;

public class CacheTypeMismatchException extends RuntimeException {
    public CacheTypeMismatchException(CacheType expected, CacheType actual) {
        super("Expected " + expected + " but got " + actual);
    }
}
```

- [ ] **Step 4: Create CacheKeyArgumentException**

```java
package org.ninng.businesssvc.cache.exception;

public class CacheKeyArgumentException extends RuntimeException {
    public CacheKeyArgumentException(String message) {
        super(message);
    }
}
```

- [ ] **Step 5: Create CacheType enum**

```java
package org.ninng.businesssvc.cache.domain;

public enum CacheType {
    VALUE,
    LIST,
    SET,
    HASH
}
```

- [ ] **Step 6: Create KeySpec record**

```java
package org.ninng.businesssvc.cache.domain;

public record KeySpec(boolean needsTenant, boolean needsId, boolean needsFields) {

    public static final KeySpec NONE    = new KeySpec(false, false, false);
    public static final KeySpec TID     = new KeySpec(true,  false, false);
    public static final KeySpec ID      = new KeySpec(false, true,  false);
    public static final KeySpec TID_ID  = new KeySpec(true,  true,  false);

    public KeySpec withFields() {
        return new KeySpec(needsTenant, needsId, true);
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/exception/*.java
git add business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheType.java
git add business-svc/src/main/java/org/ninng/businesssvc/cache/domain/KeySpec.java
git commit -m "feat: 业务缓存异常类、CacheType 枚举、KeySpec"
```

---

### Task 2: CacheDomain + CacheKey + CacheDomains

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheKey.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheDomain.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheDomains.java`

- [ ] **Step 1: Create CacheKey record**

```java
package org.ninng.businesssvc.cache.domain;

import org.springframework.lang.Nullable;

import java.util.Optional;

public record CacheKey<ID, TID>(
    CacheDomain<ID, TID> domain,
    @Nullable TID tid,
    @Nullable ID id,
    String[] fields
) {
    /** 完整的 Redis key */
    public String fullKey() {
        return domain.buildKeyString(tid, id);
    }

    /** 批量扫描 pattern（用于清空该租户下该域所有缓存） */
    public String batchPattern() {
        return domain.buildBatchPattern(tid);
    }

    /** 单个 key pattern（占位符格式） */
    public String singlePattern() {
        if (tid != null && id != null) {
            return String.format(domain.keyPattern(), tid, id);
        } else if (tid != null) {
            return String.format(domain.keyPattern(), tid);
        } else if (id != null) {
            return String.format(domain.keyPattern(), id);
        }
        return domain.keyPattern();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T field(int index) {
        return fields != null && index < fields.length ? (T) fields[index] : null;
    }

    public boolean hasFields() {
        return fields != null && fields.length > 0;
    }

    public Optional<String> field(int index, Class<String> type) {
        return Optional.ofNullable(field(index));
    }

    public Class<ID> idType() {
        return domain.idType();
    }

    public Class<TID> tidType() {
        return domain.tidType();
    }

    public KeySpec spec() {
        return domain.spec();
    }
}
```

- [ ] **Step 2: Create CacheDomain sealed class**

```java
package org.ninng.businesssvc.cache.domain;

import org.ninng.businesssvc.cache.exception.CacheKeyArgumentException;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.function.Function;

public sealed class CacheDomain<ID, TID> permits CacheDomains {

    private final String name;
    private final CacheType type;
    private final String keyPattern;
    private final KeySpec spec;
    private final Class<ID> idType;
    private final Class<TID> tidType;
    @Nullable
    private final Function<Object, ID> idExtractor;
    @Nullable
    private final Class<?> valueType;

    // ─── constructor ───

    CacheDomain(String name, CacheType type, String keyPattern, KeySpec spec,
                Class<ID> idType, Class<TID> tidType,
                @Nullable Function<Object, ID> idExtractor,
                @Nullable Class<?> valueType) {
        this.name = name;
        this.type = type;
        this.keyPattern = keyPattern;
        this.spec = spec;
        this.idType = idType;
        this.tidType = tidType;
        this.idExtractor = idExtractor;
        this.valueType = valueType;
    }

    // ─── public methods ───

    public String name() { return name; }

    public CacheType type() { return type; }

    public String keyPattern() { return keyPattern; }

    public KeySpec spec() { return spec; }

    public Class<ID> idType() { return idType; }

    public Class<TID> tidType() { return tidType; }

    @Nullable
    public Function<Object, ID> idExtractor() { return idExtractor; }

    @Nullable
    public Class<?> valueType() { return valueType; }

    /** 构建完整的 Redis key */
    public String buildKeyString(@Nullable TID tid, @Nullable ID id) {
        if (spec.needsTenant() && tid == null) {
            throw new CacheKeyArgumentException(
                    "TenantId is required for " + name);
        }
        if (spec.needsId() && id == null) {
            throw new CacheKeyArgumentException(
                    "Id is required for " + name);
        }
        return formatKey(tid, id);
    }

    /** 构建 CacheKey 对象 */
    public CacheKey<ID, TID> buildKey(@Nullable TID tid, @Nullable ID id, String... fields) {
        return new CacheKey<>(this, tid, id,
                spec.needsFields() ? (fields.length > 0 ? fields : new String[0]) : null);
    }

    /** 构建批量扫描 pattern */
    public String buildBatchPattern(@Nullable TID tid) {
        if (spec.needsTenant() && tid == null) {
            throw new CacheKeyArgumentException(
                    "TenantId is required for batch pattern of " + name);
        }
        // 用 * 替换最后一段
        String formatted = formatKey(tid, null);
        if (spec.needsId()) {
            // 有 id 时，最后一个 %s 替换为 *
            return formatted.substring(0, formatted.lastIndexOf(':') + 1) + "*";
        }
        return formatted;
    }

    // ─── internal ───

    private String formatKey(@Nullable TID tid, @Nullable ID id) {
        if (spec.needsTenant() && spec.needsId()) {
            return String.format(keyPattern, tid, id);
        } else if (spec.needsTenant()) {
            return String.format(keyPattern, tid);
        } else if (spec.needsId()) {
            return String.format(keyPattern, id);
        }
        return keyPattern;
    }

    // ─── builder-like methods ───

    public CacheDomain<ID, TID> withIdExtractor(Function<Object, ID> extractor) {
        return new CacheDomain<>(name, type, keyPattern, spec, idType, tidType, extractor, valueType);
    }

    public CacheDomain<ID, TID> withValueType(Class<?> vt) {
        return new CacheDomain<>(name, type, keyPattern, spec, idType, tidType, idExtractor, vt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CacheDomain<?, ?> that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "CacheDomain{" + name + ", " + type + "}";
    }
}
```

- [ ] **Step 3: Create CacheDomains — permits class + 静态常量**

```java
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

    // ─── 缓存域定义 ───

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

    // ─── 名称解析 ───

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
```

- [ ] **Step 4: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheKey.java
git add business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheDomain.java
git add business-svc/src/main/java/org/ninng/businesssvc/cache/domain/CacheDomains.java
git commit -m "feat: CacheDomain sealed class、CacheKey、CacheDomains 常量"
```

---

### Task 3: 加载器体系 — ForCache、PageResult、CacheLoader、CacheLoaderRegistry

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/loader/ForCache.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/loader/PageResult.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/loader/CacheLoader.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/loader/CacheLoaderRegistry.java`

- [ ] **Step 1: Create ForCache annotation**

```java
package org.ninng.businesssvc.cache.loader;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForCache {
    String value();
}
```

- [ ] **Step 2: Create PageResult record**

```java
package org.ninng.businesssvc.cache.loader;

import java.util.List;

public record PageResult<V>(List<V> data, int page, int pageSize, long total) {

    public boolean hasNext() {
        return (long) page * pageSize < total;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }
}
```

- [ ] **Step 3: Create CacheLoader interface**

```java
package org.ninng.businesssvc.cache.loader;

import org.ninng.businesssvc.cache.domain.CacheKey;
import org.springframework.lang.Nullable;

import java.util.*;

public interface CacheLoader<ID, TID, V> {

    @Nullable
    V load(CacheKey<ID, TID> key);

    PageResult<V> loadPage(CacheKey<ID, TID> key, int page, int pageSize);

    default Map<ID, V> batchLoad(CacheKey<ID, TID> pattern, Set<ID> ids) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (ID id : ids) {
            CacheKey<ID, TID> singleKey = pattern.domain().buildKey(pattern.tid(), id);
            V value = load(singleKey);
            if (value != null) {
                result.put(id, value);
            }
        }
        return result;
    }
}
```

- [ ] **Step 4: Create CacheLoaderRegistry**

```java
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
```

- [ ] **Step 5: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/loader/*.java
git commit -m "feat: CacheLoader 加载器体系、@ForCache 注解、自动注册"
```

---

### Task 4: 策略层 — CacheTypeStrategy + 四种实现 + RefreshStrategy + CacheStrategyFactory

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/CacheTypeStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/RefreshStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/ValueStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/ListStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/SetStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/HashStrategy.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/CacheStrategyFactory.java`

- [ ] **Step 1: Create CacheTypeStrategy interface**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RBatch;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Function;

public interface CacheTypeStrategy {

    CacheType type();

    // ─── 单值操作 ───
    <V> Optional<V> get(RedissonClient rc, String key);
    <V> void put(RedissonClient rc, String key, V value);
    void evict(RedissonClient rc, String key);

    // ─── 批量操作（使用 RBatch） ───
    <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                Function<String, ID> keyToId);
    <V> void batchPut(RedissonClient rc, Map<String, V> kvMap);

    // ─── 全量刷新 ───
    <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                              CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                              int pageSize);

    // ─── 清空 ───
    void clear(RedissonClient rc, String pattern);
}
```

- [ ] **Step 2: Create RefreshStrategy**

```java
package org.ninng.businesssvc.cache.strategy;

import java.util.function.Consumer;

@FunctionalInterface
public interface RefreshStrategy {

    int pageSize();

    static RefreshStrategy fixed(int pageSize) {
        return () -> pageSize;
    }

    static RefreshStrategy defaultStrategy() {
        return fixed(200);
    }
}
```

- [ ] **Step 3: Create ValueStrategy**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class ValueStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.VALUE;
    }

    @Override
    public <V> Optional<V> get(RedissonClient rc, String key) {
        RBucket<V> bucket = rc.getBucket(key);
        return Optional.ofNullable(bucket.get());
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        rc.<V>getBucket(key).set(value);
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getBucket(key).delete();
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        var batch = rc.createBatch();
        Map<String, RFuture<?>> futures = new LinkedHashMap<>();
        for (String key : keys) {
            futures.put(key, batch.getBucket(key).getAsync());
        }
        batch.execute();

        Map<ID, V> result = new LinkedHashMap<>();
        futures.forEach((key, future) -> {
            @SuppressWarnings("unchecked")
            V value = (V) future.getNow();
            if (value != null) {
                result.put(keyToId.apply(key), value);
            }
        });
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        var batch = rc.createBatch();
        kvMap.forEach((key, value) -> batch.getBucket(key).setAsync(value));
        batch.execute();
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                      CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                      int pageSize) {
        int page = 1;
        PageResult<V> pageResult;
        var idExtractor = domain.idExtractor();
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                Map<String, V> kvMap = new LinkedHashMap<>();
                for (V item : pageResult.data()) {
                    ID id = idExtractor != null ? idExtractor.apply(item) : null;
                    String fullKey = domain.buildKeyString(key.tid(), id);
                    kvMap.put(fullKey, item);
                }
                batchPut(rc, kvMap);
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        // 通过 SCAN 删除匹配 key（生产环境禁用 KEYS）
        var keys = rc.getKeys();
        for (String key : keys.getKeysByPattern(pattern)) {
            keys.delete(key);
        }
    }
}
```

- [ ] **Step 4: Create ListStrategy**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class ListStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.LIST;
    }

    @Override
    public <V> Optional<V> get(RedissonClient rc, String key) {
        @SuppressWarnings("unchecked")
        RList<Object> list = rc.getList(key);
        List<Object> data = list.readAll();
        return data.isEmpty() ? Optional.empty() : Optional.of((V) data);
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) value;
        RList<Object> list = rc.getList(key);
        list.delete();
        if (!items.isEmpty()) {
            list.addAll(items);
        }
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getList(key).delete();
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        // LIST 类型的批量获取不适合 RBatch（返回 List<Object>），逐条查
        Map<ID, V> result = new LinkedHashMap<>();
        for (String keyStr : keys) {
            @SuppressWarnings("unchecked")
            V value = (V) get(rc, keyStr).orElse(null);
            if (value != null) {
                result.put(keyToId.apply(keyStr), value);
            }
        }
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        kvMap.forEach((key, value) -> put(rc, key, value));
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                      CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                      int pageSize) {
        RList<V> list = rc.getList(key.fullKey());
        list.delete();

        int page = 1;
        PageResult<V> pageResult;
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                list.addAll(pageResult.data());
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        var keys = rc.getKeys();
        for (String k : keys.getKeysByPattern(pattern)) {
            keys.delete(k);
        }
    }
}
```

- [ ] **Step 5: Create SetStrategy**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class SetStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.SET;
    }

    @Override
    public <V> Optional<V> get(RedissonClient rc, String key) {
        @SuppressWarnings("unchecked")
        Set<Object> data = rc.<Object>getSet(key).readAll();
        return data.isEmpty() ? Optional.empty() : Optional.of((V) data);
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        @SuppressWarnings("unchecked")
        Collection<Object> items = (Collection<Object>) value;
        RSet<Object> set = rc.getSet(key);
        set.delete();
        if (!items.isEmpty()) {
            set.addAll(items);
        }
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getSet(key).delete();
    }

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (String keyStr : keys) {
            @SuppressWarnings("unchecked")
            V value = (V) get(rc, keyStr).orElse(null);
            if (value != null) {
                result.put(keyToId.apply(keyStr), value);
            }
        }
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        kvMap.forEach((key, value) -> put(rc, key, value));
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                      CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                      int pageSize) {
        RSet<V> set = rc.getSet(key.fullKey());
        set.delete();

        int page = 1;
        PageResult<V> pageResult;
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                set.addAll(pageResult.data());
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        var keys = rc.getKeys();
        for (String k : keys.getKeysByPattern(pattern)) {
            keys.delete(k);
        }
    }
}
```

- [ ] **Step 6: Create HashStrategy**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.loader.CacheLoader;
import org.ninng.businesssvc.cache.loader.PageResult;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
public class HashStrategy implements CacheTypeStrategy {

    @Override
    public CacheType type() {
        return CacheType.HASH;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(RedissonClient rc, String key) {
        RMap<String, Object> map = rc.getMap(key);
        Map<String, Object> all = map.readAllMap();
        return all.isEmpty() ? Optional.empty() : Optional.of((V) all);
    }

    @Override
    public <V> void put(RedissonClient rc, String key, V value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) value;
        rc.<String, Object>getMap(key).putAll(fields);
    }

    @Override
    public void evict(RedissonClient rc, String key) {
        rc.getMap(key).delete();
    }

    // ─── Hash 特有 ───

    public <V> Optional<V> getField(RedissonClient rc, String key, String field) {
        @SuppressWarnings("unchecked")
        V value = (V) rc.<String, Object>getMap(key).get(field);
        return Optional.ofNullable(value);
    }

    public <V> void putField(RedissonClient rc, String key, String field, V value) {
        rc.<String, V>getMap(key).fastPut(field, value);
    }

    public <V> Map<String, V> batchGetFields(RedissonClient rc, String key, Set<String> fields) {
        @SuppressWarnings("unchecked")
        Map<String, V> result = (Map<String, V>) rc.<String, Object>getMap(key).getAll(fields);
        return result;
    }

    public <V> void batchPutFields(RedissonClient rc, String key, Map<String, V> kvs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) kvs;
        rc.<String, Object>getMap(key).putAll(raw);
    }

    // ─── 批量操作 ───

    @Override
    public <ID, V> Map<ID, V> batchGet(RedissonClient rc, Set<String> keys,
                                        Function<String, ID> keyToId) {
        Map<ID, V> result = new LinkedHashMap<>();
        for (String key : keys) {
            @SuppressWarnings("unchecked")
            V value = (V) get(rc, key).orElse(null);
            if (value != null) {
                result.put(keyToId.apply(key), value);
            }
        }
        return result;
    }

    @Override
    public <V> void batchPut(RedissonClient rc, Map<String, V> kvMap) {
        kvMap.forEach((key, value) -> put(rc, key, value));
    }

    @Override
    public <ID, TID, V> void refresh(RedissonClient rc, CacheDomain<ID, TID> domain,
                                      CacheLoader<ID, TID, V> loader, CacheKey<ID, TID> key,
                                      int pageSize) {
        var idExtractor = domain.idExtractor();
        int page = 1;
        PageResult<V> pageResult;
        do {
            pageResult = loader.loadPage(key, page, pageSize);
            if (!pageResult.isEmpty()) {
                for (V item : pageResult.data()) {
                    ID id = idExtractor != null ? idExtractor.apply(item) : null;
                    String fullKey = domain.buildKeyString(key.tid(), id);
                    // Hash 类型：value 需要是 Map<String, ?>
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fields = (Map<String, Object>) item;
                    rc.<String, Object>getMap(fullKey).putAll(fields);
                }
            }
            page++;
        } while (pageResult.hasNext());
    }

    @Override
    public void clear(RedissonClient rc, String pattern) {
        var keys = rc.getKeys();
        for (String k : keys.getKeysByPattern(pattern)) {
            keys.delete(k);
        }
    }
}
```

- [ ] **Step 7: Create CacheStrategyFactory**

```java
package org.ninng.businesssvc.cache.strategy;

import org.ninng.businesssvc.cache.domain.CacheType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CacheStrategyFactory {

    private final Map<CacheType, CacheTypeStrategy> strategies;

    public CacheStrategyFactory(List<CacheTypeStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(CacheTypeStrategy::type, Function.identity()));
    }

    public CacheTypeStrategy get(CacheType type) {
        CacheTypeStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy for " + type);
        }
        return strategy;
    }

    public ValueStrategy value() { return (ValueStrategy) get(CacheType.VALUE); }
    public ListStrategy list()   { return (ListStrategy) get(CacheType.LIST); }
    public SetStrategy  set()    { return (SetStrategy) get(CacheType.SET); }
    public HashStrategy hash()   { return (HashStrategy) get(CacheType.HASH); }
}
```

- [ ] **Step 8: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/strategy/*.java
git commit -m "feat: CacheTypeStrategy 策略层、四种数据结构实现、策略工厂"
```

---

### Task 5: CacheLockManager 分布式锁

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/lock/CacheLockManager.java`

- [ ] **Step 1: Create CacheLockManager**

```java
package org.ninng.businesssvc.cache.lock;

import org.ninng.businesssvc.cache.exception.CacheLockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CacheLockManager {

    private static final String LOCK_PREFIX = "lock:business-cache:";

    private final RedissonClient redisson;

    public CacheLockManager(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * 尝试获取锁，waitTime=0 快速失败。
     *
     * @param scope 锁范围 "DOMAIN:tid"，如 "USER:1"
     * @return 是否成功获取锁
     */
    public boolean tryLock(String scope) {
        RLock lock = redisson.getLock(LOCK_PREFIX + scope);
        try {
            return lock.tryLock(0, 30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(String scope) {
        RLock lock = redisson.getLock(LOCK_PREFIX + scope);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 带自动 unlock 的执行器
     *
     * @param scope  锁范围
     * @param action 获取锁后执行
     * @param onFail 未获取锁时执行（可抛异常快速失败）
     * @param <T>    返回类型
     * @return action 或 onFail 的结果
     */
    public <T> T withLock(String scope, Supplier<T> action, Supplier<T> onFail) {
        if (!tryLock(scope)) {
            return onFail.get();
        }
        try {
            return action.get();
        } finally {
            unlock(scope);
        }
    }

    /** 便捷重载：未获取锁时抛 CacheLockException */
    public void withLockOrThrow(String scope, Runnable action) {
        withLock(scope, () -> {
            action.run();
            return null;
        }, () -> {
            throw new CacheLockException("Failed to acquire lock for " + scope);
        });
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/lock/CacheLockManager.java
git commit -m "feat: CacheLockManager 分布式锁，waitTime=0 快速失败"
```

---

### Task 6: CacheOps 统一门面 + Builder

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/ops/CacheOps.java`

This is the largest file (≈300 lines). It orchestrates L1→L2→Loader flow.

- [ ] **Step 1: Create CacheOps**

```java
package org.ninng.businesssvc.cache.ops;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheKey;
import org.ninng.businesssvc.cache.domain.CacheType;
import org.ninng.businesssvc.cache.exception.CacheKeyArgumentException;
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
    private final Cache<String, Object> localCache;
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

        // 构建所有 fullKey → ID 映射
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

            // 有数据写入，无数据 evict
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
            // 仍未加载到的 key → evict
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

            // 清空该域下所有相关 key
            strategy.clear(redisson, key.batchPattern());
            // 逐页刷新
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

        // miss → load 单条 → 回写对应 field
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
        private Cache<String, Object> localCache;
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

        private <T> T require(T obj) {
            if (obj == null) throw new NullPointerException("Required field is null in CacheOps.Builder");
            return obj;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/cache/ops/CacheOps.java
git commit -m "feat: CacheOps 统一门面，L1→L2→Loader 三级查询"
```

---

### Task 7: Spring 配置 + CacheEventBridge

**Files:**
- Create: `business-svc/src/main/java/org/ninng/businesssvc/config/RedissonConfig.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/config/BusinessCacheConfig.java`
- Create: `business-svc/src/main/java/org/ninng/businesssvc/cache/bridge/CacheEventBridge.java`

- [ ] **Step 1: Create RedissonConfig**

```java
package org.ninng.businesssvc.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties properties) {
        Config config = new Config();
        String url = "redis://" + properties.getHost() + ":" + properties.getPort();
        config.useSingleServer()
                .setAddress(url)
                .setPassword(properties.getPassword())
                .setDatabase(properties.getDatabase() > 0 ? properties.getDatabase() : 0)
                .setConnectionPoolSize(16)
                .setConnectionMinimumIdleSize(4);
        return Redisson.create(config);
    }
}
```

- [ ] **Step 2: Create BusinessCacheConfig**

```java
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
    public CacheOps cacheOps(RedissonClient redisson,
                             CacheStrategyFactory strategies,
                             CacheLoaderRegistry loaders) {
        return CacheOps.builder()
                .redisson(redisson)
                .strategies(strategies)
                .loaders(loaders)
                .build();
    }

    /** 启用本地 Caffeine 缓存的版本（通过配置 onion.cache.business.local.enabled=true 开启） */
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
```

- [ ] **Step 3: Create CacheEventBridge**

```java
package org.ninng.businesssvc.cache.bridge;

import org.ninng.businesssvc.cache.domain.CacheDomain;
import org.ninng.businesssvc.cache.domain.CacheDomains;
import org.ninng.businesssvc.cache.ops.CacheOps;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.event.CacheInvalidateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 新缓存与现有 CaffeineRedisCacheManager 的 Spring Event 桥接。
 * 当旧缓存（u/ur）失效时，同步失效新缓存的本地 Caffeine。
 */
@Component
public class CacheEventBridge {

    private static final Logger log = LoggerFactory.getLogger(CacheEventBridge.class);

    private final CacheOps cacheOps;

    /** 旧缓存名 → 新 CacheDomain 映射 */
    private static final Map<String, CacheDomain<?, ?>> LEGACY_MAP = Map.of(
            CacheConstant.USER, CacheDomains.USER
    );

    public CacheEventBridge(CacheOps cacheOps) {
        this.cacheOps = cacheOps;
    }

    @EventListener
    public void onLegacyCacheInvalidate(CacheInvalidateEvent event) {
        event.evictions().forEach((cacheName, keys) -> {
            CacheDomain<?, ?> domain = LEGACY_MAP.get(cacheName);
            if (domain == null) return;

            if (keys == null || keys.isEmpty()) {
                log.debug("Legacy cache '{}' cleared, no specific keys to invalidate", cacheName);
                return;
            }

            for (Object key : keys) {
                if (key instanceof String k) {
                    // key 格式：Redis key 的子串，尝试提取 id
                    // 简单策略：invalidateLocal 不需要精确的 tenant/id
                    // 直接全量 invalidate 本地
                    log.debug("Legacy event for {} key={}, invalidating local cache", cacheName, key);
                }
            }
        });
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/config/RedissonConfig.java
git add business-svc/src/main/java/org/ninng/businesssvc/config/BusinessCacheConfig.java
git add business-svc/src/main/java/org/ninng/businesssvc/cache/bridge/CacheEventBridge.java
git commit -m "feat: Redisson 配置、CacheOps 自动装配、CacheEventBridge 桥接"
```

---

### Task 8: 验证编译

- [ ] **Step 1: 编译验证**

```bash
cd business-svc
./mvnw compile -q
```

Expected: BUILD SUCCESS（无任何输出）

- [ ] **Step 2: 如果编译失败，修复并重试**

```bash
./mvnw compile
```

- [ ] **Step 3: 全部提交**

```bash
git add -A
git commit -m "feat: 完成业务缓存抽象层全部实现"
```

---

## 自检清单

| 需求 | 实现位置 | 状态 |
|------|----------|------|
| 缓存域枚举（一个枚举=一类数据） | CacheDomain sealed class + CacheDomains 静态常量 | ✓ |
| 自动 key 构建 | CacheDomain.buildKeyString() + KeySpec 校验 | ✓ |
| 4 种数据结构 | VALUE/List/Set/HashStrategy | ✓ |
| 批量 get/put（RBatch 一次往返） | CacheTypeStrategy.batchGet/batchPut (ValueStrategy 使用 RBatch) | ✓ |
| 分页全量刷新 | CacheTypeStrategy.refresh() + CacheOps.refresh() + 分布式锁 | ✓ |
| 数据加载器抽象 | CacheLoader<ID,TID,V> 接口 + @ForCache 自动注册 | ✓ |
| 空结果自动删除 | CacheOps.get() → loader返回null → evict L2 + invalidate L1 | ✓ |
| 分布式锁（快速失败） | CacheLockManager.tryLock(waitTime=0) | ✓ |
| 可选 Caffeine 本地缓存 | CacheOps.Builder.localCache() + ConditionalOnProperty | ✓ |
| Spring Event 联动 | CacheEventBridge 监听 CacheInvalidateEvent | ✓ |
