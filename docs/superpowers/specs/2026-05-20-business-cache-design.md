# 业务缓存抽象层设计

日期：2026-05-20

## 概述

设计一个业务缓存抽象层，用于缓存业务层数据（租户信息、用户信息等），与现有的 `CaffeineRedisCacheManager`（Caffeine L1 + Redis L2）互补。新抽象负责复杂业务数据场景（list/set/hash、批量操作、分页刷新），现有 `@Cacheable` 继续用于简单 ORM 实体缓存。两者通过 Spring Event 桥接联动。

## 核心设计决策

| 点 | 决策 |
|---|---|
| 架构模式 | 策略工厂 + 泛型门面 + Spring Bean 自动注册 |
| 域定义 | `sealed class CacheDomain<ID, TID>` + 静态常量（枚举不支持泛型） |
| Key 构建 | `String.format(keyPattern, tid, id, ...)` 自动构建，`KeySpec` 校验参数 |
| 数据结构 | VALUE (RBucket), LIST (RList), SET (RSet), HASH (RMap) |
| 加载器注册 | `@ForCache` 注解 → `CacheLoaderRegistry` 自动收集 Spring Bean |
| 本地缓存 | 可选 Caffeine，默认不启用 |
| 分布式锁 | Redisson RLock，仅全量刷新时加锁，waitTime=0 快速失败 |
| 空结果行为 | Loader 返回 null → 自动 evict (L2) + invalidate (L1)，不缓存空标记 |
| 批量操作 | Redisson RBatch，一次网络往返 |
| 刷新分页 | 默认 pageSize=200，逐页 RBatch 写入，策略可覆写 |

## 架构总览

```
调用方
  │
  ▼
CacheOps (泛型门面)
  │
  ├─► CacheDomain<ID, TID> ─── sealed class 静态常量：USER, TENANT, DEPT...
  │     ├─ CacheType                VALUE / LIST / SET / HASH
  │     ├─ keyPattern               如 "onion:user:%s:%s"
  │     ├─ KeySpec                  参数校验：needsTenant / needsId / needsFields
  │     └─ RefreshStrategy         分页策略（可选覆写）
  │
  ├─► [可选] Caffeine ─── 本地缓存 L1
  │
  ├─► RedissonClient ─── Redis 数据操作 L2
  │     │
  │     └─► CacheStrategyFactory ── 按 CacheType 选择策略
  │           ├─ ValueStrategy (RBucket)
  │           ├─ ListStrategy  (RList)
  │           ├─ SetStrategy   (RSet)
  │           └─ HashStrategy  (RMap)
  │
  ├─► CacheLoaderRegistry ─── @ForCache 自动收集
  │     └─ CacheLoader<ID, TID, V>  ── 用户实现
  │
  ├─► CacheLockManager ─── Redisson RLock
  │
  └─► CacheEventBridge ─── Spring Event 与旧缓存联动
```

## 组件详细设计

### 1. CacheDomain<ID, TID> — 缓存域定义

```
文件: cache/domain/CacheDomain.java
      cache/domain/CacheDomains.java
      cache/domain/CacheType.java
      cache/domain/KeySpec.java
      cache/domain/CacheKey.java
```

sealed class + 静态常量，提供枚举式的"一类数据一个域"语义，同时支持 `<ID, TID>` 泛型。

```java
public sealed class CacheDomain<ID, TID> permits CacheDomains {
    public static final CacheDomain<Long, Long> USER =
        new CacheDomain<>(CacheType.HASH,  "onion:user:%s:%s",    KeySpec.TID_ID)
            .withIdExtractor(v -> ((UserView) v).id());

    public static final CacheDomain<Long, Long> TENANT =
        new CacheDomain<>(CacheType.VALUE, "onion:tenant:%s",      KeySpec.TID);

    public static final CacheDomain<Long, Long> DEPT =
        new CacheDomain<>(CacheType.LIST,  "onion:dept:%s:list",  KeySpec.TID);

    public static final CacheDomain<String, Void> SYS_CONFIG =
        new CacheDomain<>(CacheType.VALUE, "onion:config:%s",      KeySpec.ID);
}
```

`KeySpec` 描述构建 key 需要的参数组合：
- `NONE` — 无租户，无主键（全局 key）
- `TID` — 仅租户
- `ID` — 仅主键
- `TID_ID` — 租户 + 主键
- 链式 `.withFields()` 标记 Hash 需要 sub-field

`CacheKey<ID, TID>` 接口提供：`fullKey()`, `tid()`, `id()`, `fields()`, `batchPattern()`, `singlePattern()`。

`CacheDomain.buildKey(tid, id)` 自动校验 `KeySpec` 参数完整性，不合法时抛 `CacheKeyArgumentException`。

**刷新时的 ID 提取**：`CacheDomain` 持有 `idExtractor: Function<Object, ID>`，用户通过 `.withIdExtractor(v -> ((UserView) v).id())` 注册。全量刷新加载分页数据后，调用 extractor 从每条 value 中提取主键，用于拼装 `{domain}_{id}` 格式的缓存 key，写入 Redisson。

**值类型感知**：`CacheOps` 通过 `domain.valueClass()` 获取值类型（运行时从 loader 的泛型信息或 domain 配置推断），传给 `CacheTypeStrategy.get()` 供反序列化。默认回退为 `Object.class`。

### 2. CacheLoader — 加载器接口

```
文件: cache/loader/CacheLoader.java
      cache/loader/ForCache.java
      cache/loader/CacheLoaderRegistry.java
      cache/loader/PageResult.java
```

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForCache {
    String value(); // 域名称，如 "USER"
}

public interface CacheLoader<ID, TID, V> {
    @Nullable V load(CacheKey<ID, TID> key);
    PageResult<V> loadPage(CacheKey<ID, TID> key, int page, int pageSize);
    default Map<ID, V> batchLoad(CacheKey<ID, TID> pattern, Set<ID> ids) { ... }
}
```

`CacheLoaderRegistry` 在构造时收集所有带 `@ForCache` 注解的 `CacheLoader` Bean，建立 `Map<CacheDomain, CacheLoader>` 映射。重复注册同名域会抛异常。

### 3. CacheTypeStrategy — 策略工厂

```
文件: cache/strategy/CacheTypeStrategy.java
      cache/strategy/{Value,List,Set,Hash}Strategy.java
      cache/strategy/CacheStrategyFactory.java
      cache/strategy/RefreshStrategy.java
```

每种 `CacheType` 一个策略实现，封装 Redisson 原生数据结构的操作方式：

| 策略 | Redisson 类型 | 适用场景 |
|---|---|---|
| ValueStrategy | RBucket | 单条记录（租户信息、配置项） |
| ListStrategy | RList | 有序列表（部门列表） |
| SetStrategy | RSet | 去重集合（用户ID集合） |
| HashStrategy | RMap | 带子字段的对象（用户详情 field:name/email/roles) |

HashStrategy 额外提供：`getField()`, `putField()`, `batchGetFields()`, `batchPutFields()`。

所有批量操作使用 `RBatch` 合并为一次网络往返，不逐条发送。

`RefreshStrategy` 控制刷新行为：
- `pageSize()` — 默认 200
- `batchMode()` — PER_PAGE / EVERY_N_PAGES / END_OF_REFRESH
- 用户可通过 `domain.withRefresh(custom)` 覆写

### 4. CacheOps — 统一门面

```
文件: cache/ops/CacheOps.java
```

对外暴露所有缓存操作，内部编排 L1→L2→Loader 三级查询流程。

**核心方法**：
```java
// 单条
<ID,TID,V> Optional<V>  get(CacheDomain<ID,TID> domain, TID tid, ID id)
<ID,TID,V> void         put(CacheDomain<ID,TID> domain, TID tid, ID id, V value)
<ID,TID>   void         evict(CacheDomain<ID,TID> domain, TID tid, ID id)

// 批量
<ID,TID,V> Map<ID,V>    batchGet(CacheDomain<ID,TID> domain, TID tid, Set<ID> ids)
<ID,TID,V> void         batchPut(CacheDomain<ID,TID> domain, TID tid, Map<ID,V> data)

// 全量刷新（加分布式锁）
<ID,TID>   void         refresh(CacheDomain<ID,TID> domain, TID tid)

// Hash 扩展
<ID,TID,V> Optional<V>  hget(CacheDomain<ID,TID> domain, TID tid, ID id, String field)
<ID,TID,V> void         hput(CacheDomain<ID,TID> domain, TID tid, ID id, String field, V value)
<ID,TID,V> Map<String,V> hgetAll(CacheDomain<ID,TID> domain, TID tid, ID id, Set<String> fields)
<ID,TID,V> void         hputAll(CacheDomain<ID,TID> domain, TID tid, ID id, Map<String,V> kvs)
```

**三级查询流程（GET）**：
1. L1 (Caffeine) 命中 → 返回
2. L2 (Redisson) 命中 → 回填 L1 → 返回
3. Loader.load() → 有值：写 L2 + L1 → 返回
4. Loader 返回 null → evict L2 + invalidate L1 → 返回 empty

**批量获取流程（BATCH_GET）**：
1. Caffeine 过滤命中
2. RBatch 批量查 Redisson 缺失 key
3. 仍未命中 → loader.batchLoad()
4. 有数据写回，无数据 evict

**构建器模式**：`CacheOps.builder().redisson(rc).loaders(registry).localCache(c -> c.maximumSize(10000)).build()`

`localCache` 为 null 时跳过 L1，直接查 Redisson。

### 5. CacheLockManager — 分布式锁

```
文件: cache/lock/CacheLockManager.java
```

```java
public boolean tryLock(String scope) {
    RLock lock = redisson.getLock("lock:business-cache:" + scope);
    return lock.tryLock(0, 30, TimeUnit.SECONDS); // waitTime=0 → 快速失败
}
```

锁范围：`"{domain}:{tid}"`，如 `"USER:1"`。仅全量 `refresh()` 时使用。获取锁失败抛出 `CacheLockException`。

### 6. CacheEventBridge — Spring Event 桥接

```
文件: cache/bridge/CacheEventBridge.java
```

双向桥接：
- **新 → 旧**：refresh 后可发布 `CacheInvalidateEvent.Local`，通知现有 `CaffeineRedisCacheManager` 失效
- **旧 → 新**：监听 `CacheInvalidateEvent`，通过 cacheName 映射找到对应 `CacheDomain`，invalidate 本地 Caffeine

### 7. 异常类型

```
文件: cache/exception/CacheLockException.java
      cache/exception/CacheLoaderNotFoundException.java
      cache/exception/CacheTypeMismatchException.java
      cache/exception/CacheKeyArgumentException.java
```

### 8. Spring 自动配置

```
文件: config/RedissonConfig.java
      config/BusinessCacheConfig.java
```

- `RedissonConfig` — 创建 `RedissonClient` Bean
- `BusinessCacheConfig` — 创建 `CacheOps` Bean（组装 Redisson、策略工厂、加载器注册表）
- `CacheLoaderRegistry` — 自动扫描 `@ForCache` Bean
- `CacheStrategyFactory` — 自动收集 `CacheTypeStrategy` Bean

## 目录结构

```
business-svc/src/main/java/org/ninng/businesssvc/
├── cache/
│   ├── domain/           CacheDomain, CacheDomains, CacheKey, CacheType, KeySpec
│   ├── loader/           CacheLoader, ForCache, CacheLoaderRegistry, PageResult
│   ├── strategy/          CacheTypeStrategy, ValueStrategy, ListStrategy, SetStrategy,
│   │                     HashStrategy, CacheStrategyFactory, RefreshStrategy
│   ├── lock/             CacheLockManager
│   ├── ops/              CacheOps (+ Builder)
│   ├── bridge/           CacheEventBridge
│   └── exception/        CacheLockException, CacheLoaderNotFoundException,
│                         CacheTypeMismatchException, CacheKeyArgumentException
└── config/
    ├── RedissonConfig.java
    └── BusinessCacheConfig.java
```

## 典型使用示例

```java
// 1. 实现加载器
@ForCache("USER")
@Component
public class UserCacheLoader implements CacheLoader<Long, Long, UserView> {

    private final UserRepository userRepo;

    @Override
    public UserView load(CacheKey<Long, Long> key) {
        return userRepo.findByTenantAndId(key.tid(), key.id());
    }

    @Override
    public PageResult<UserView> loadPage(CacheKey<Long, Long> key, int page, int pageSize) {
        var result = userRepo.findPageByTenant(key.tid(), page, pageSize);
        return new PageResult<>(result.getData(), page, pageSize, result.getTotal());
    }

    @Override
    public Map<Long, UserView> batchLoad(CacheKey<Long, Long> pattern, Set<Long> ids) {
        return userRepo.findByIds(pattern.tid(), ids);
    }
}

// 2. 注入使用
@Service
public class UserService {
    private final CacheOps cacheOps;

    public UserView getUser(Long tenantId, Long userId) {
        return cacheOps.get(CacheDomains.USER, tenantId, userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public void refreshCache(Long tenantId) {
        cacheOps.refresh(CacheDomains.USER, tenantId);
    }
}
```
