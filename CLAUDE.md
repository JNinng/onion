# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
./mvnw clean install -DskipTests

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BusinessSvcApplicationTests

# Start dev infrastructure (PostgreSQL + Redis)
docker compose -f compose.yaml up -d
```

All commands above run from `business-svc/`. The root `pom.xml` is a thin parent (version/encoding only); the `business-svc/pom.xml` is self-contained with all dependencies and the Spring Boot plugin.

## Project Structure

Multi-module Maven project (`pom.xml` at root + `business-svc/` submodule). The root `pom.xml` is a thin parent; all real code lives in `business-svc/`.

### Layer Architecture (Onion)

```
dto/           → Jimmer DTO language files (*.dto) — define views, inputs, specifications
controller/    → REST endpoints (@RestController), Jimmer @FetchBy annotations
service/       → Business logic orchestration
repository/    → Data access (extends AbstractJavaRepository from Jimmer)
model/         → Jimmer ORM entities (@Entity interfaces)
  common/      → Mixin interfaces: CreatedAware, UpdatedAware, StatusAware, TenantAware, OwnerAware
model/filter/  → Jimmer draft interceptors, data-scope filters, tenant filters
security/      → Request encryption/decryption (AES, RSA, HMAC), algorithm strategy pattern
utils/         → Custom ID generators (TenantIdGenerator: 16-char Base32 IDs; SnowflakeIdGenerator: Long Snowflake IDs; both implement Jimmer UserIdGenerator)
component/     → JWT utils, i18n, auth filter, locale resolver
config/        → Spring @Configuration: Security, Jimmer cache, Jackson, CaffeineRedis cache, Redis Pub/Sub invalidation, i18n
filter/        → OncePerRequestFilter chain (trace ID, user context, security context, etc.)
handler/       → @RestControllerAdvice global exception handler
context/       → ThreadLocal holders (UserContext, SecurityContext, LinkContext, SpringContext)
constant/      → App-wide constants (cache keys, cache channel, HTTP headers, date formats)
event/        → Cache invalidation events & Spring Event listener/publisher
entity/        → POJO DTOs for request/response (R<T>, LoginResp, PageReq, etc.)
```

### Key Architectural Patterns

**1. Unified Response `R<T>`** — All controllers return `R<T>` with `code`, `msg`, `data`, and optional `algorithm` fields. Wraps success/fail/err.

**2. Jimmer ORM** — Entities are Java interfaces with `@Entity`. Keys: `@Id` with `UserIdGenerator` implementations (`TenantIdGenerator` for 16-char Base32 strings, `SnowflakeIdGenerator` for Long Snowflake IDs), `@ManyToMany` with `@JoinTable`, mixin interfaces (`CreatedAware`, `UpdatedAware`, `TenantAware`, `OwnerAware`, etc.). Jimmer DTO language (`.dto` files) generates views, inputs, and specifications.

**3. Jimmer Fetchers** — Field-level fetch control via `SysUserFetcher.$.allScalarFields().password(false).roles(...)`. Combined with `@FetchBy` on controller method return types for runtime fetch-plan enforcement.

**4. Two-Tier Caching** — `CaffeineRedisCacheManager` (primary `CacheManager`) chains local Caffeine cache (L1, 20min TTL) with Redis (L2, 1day TTL) for manual caching. Jimmer ORM uses its own `ChainCacheBuilder` for entity caching. Cache key prefix: `CacheConstant.KEY_PREFIX` → `"onion"`. Redis channel `CacheConstant.INVALIDATION_CHANNEL` → `"onion:cache:invalidate"`.

### Caching Guide

**Two-tier architecture:** Queries check L1 → L2 → valueLoader; writes sync both layers; evicts clear Redis first then Caffeine. Spring `@Cacheable` repositories benefit from this automatically.

**Cache names** (defined in `CacheConstant`): `USER` = `"u"` (user by username), `USER_ROLE` = `"ur"` (user-role by userId). `KEY_PREFIX` = `"onion"`. `INVALIDATION_CHANNEL` = `"onion:cache:invalidate"`.

**Declarative caching** via `@Cacheable` on repository methods:

```java
@Cacheable(cacheNames = CacheConstant.USER, key = "#username")
public UserDetailsView findByUsername(String username) { ... }
```

**When to evict:** Any data mutation that affects a cached query result must invalidate. E.g., `UserService.update()` changes a user → evict `u` cache; role assignment changes → evict `ur` cache.

**Programmatic single eviction:**

```java
cacheEventPublisher.evict(CacheConstant.USER, username);
cacheEventPublisher.clear(CacheConstant.USER);
```

**Batch eviction** (multiple areas/keys merged into one event):

```java
cacheEventPublisher.batch()
    .evict(CacheConstant.USER, username)
    .evict(CacheConstant.USER_ROLE, userId)
    .clear(CacheConstant.USER)
    .publish();
```

**Event types and flow:** `CacheInvalidateEvent` is a `sealed class` with two subclasses — `Local` (published by `CacheEventPublisher`) and `Redis` (published by `RedisCacheInvalidationConfig` when receiving external messages). `CacheEventListener` listens for both.

```
Service mutation → CacheEventPublisher → CacheInvalidateEvent.Local
  → CacheEventListener evicts L1+L2 via CacheManager
    → publishes to Redis channel (CacheConstant.INVALIDATION_CHANNEL) with instanceId
      → All instances (including self) receive the Redis message
        → RedisCacheInvalidationConfig → CacheInvalidateEvent.Redis (carries sourceInstanceId)
          → CacheEventListener: if sourceInstanceId == own instanceId → skip (already evicted locally)
          → otherwise → evict L1+L2
```

**Cross-instance invalidation:** Enable with `onion.cache.invalidation.redis.enabled=true`. Each instance generates a `UUID instanceId` at startup. LOCAL events publish to Redis carrying the `instanceId`; REDIS events carry a `sourceInstanceId`. The listener skips events where `sourceInstanceId` matches the local `instanceId` to avoid double-eviction. IDE autocomplete for this property is configured via `META-INF/additional-spring-configuration-metadata.json`.

**Caffeine tuning** in `CacheConfig.caffeineCacheManager()`: `initialCapacity`, `maximumSize`, `expireAfterWrite`. Redis TTL in `cacheManager()`: `entryTtl(Duration.ofDays(1))`.

**5. Filter Chain Pattern** — `MainOnceFilterHandler` collects all `OnceFilterHandler` beans and runs `before()` before the filter chain, `after()` in the finally block. Implementations: `RequestIdOnceFilterHandler` (trace ID), `SecurityContextOnceFilterHandler` (encryption algorithm), `UserContextOnceFilterHandler` (cleanup).

**6. Request Encryption** — `SecureRequestAdvice` / `SecureResponseAdvice` via `@ControllerAdvice`. Strategy pattern via `AlgorithmHandler` interface (implementations: NONE, AES, etc.). `@Security(enabled=false)` annotation skips decryption. Supports client-negotiated algorithm via `Accept-Algorithm` header.

**7. Context Holders** — `UserContextHolder`, `SecurityContextHolder`, `LinkContextHolder` manage per-request state using `TransmittableThreadLocal`. Spring Boot default profile is `dev`.

**8. i18n** — `I18nUtil` wraps Spring `MessageSource`. Locale resolved via `Accept-Language` header (`RequestLocaleResolver`). Properties files at `resources/i18n/messages*.properties`.

**9. Custom ID Generation** — Two `UserIdGenerator` implementations:
- **`TenantIdGenerator<String>`** — 16-character fixed-length Base32 IDs. The 80-bit payload (40-bit timestamp, 10-bit machine ID, 12-bit sequence, 18-bit random) is obfuscated via a **4-round Feistel permutation** (bijective, 1:1). Round keys are derived from the `time-mask` seed using a split-mix sequence. Alphabet: `23456789ABCDEFGHJKLMNPQRSTUVWXYZ` (excludes 0/O/1/I to avoid visual ambiguity). Constants `C.TENANT_ID_LENGTH` (16) and `C.TENANT_ID_ALPHABET` define the length and character set.
- **`SnowflakeIdGenerator<Long>`** — Snowflake-style distributed ID via `yitter-idgenerator`. 13-bit worker ID. Used as the primary key generator for all business entities (SysUser, SysRole, SysDept).

Both initialized at startup via `TenantIdConfig` (`@Configuration`) which reads `security.tenant-id.machine-id` and `security.tenant-id.time-mask` from `application.yaml`.

### Domain Model

- **SysUser** — id (Long, Snowflake), name, nickname, password, roles (M2M via user_role_mapping)
- **SysRole** — id (Long, Snowflake), RBAC roles with `code`, `roleType` (system/tenant), `dataScope` (data range rules 1-6), `scopeDeptId` (org visibility), and `remark`
- **SysTenant** — Multi-tenant support with `id` (String, 16-char Base32 via TenantIdGenerator), `code` and `name`
- **SysDept** — id (Long, Snowflake), Department hierarchy with `ownerUser` (was `adminUser`)
- All entities mix in `CreatedAware`, `UpdatedAware`, `StatusAware`, `TenantAware`; some also mix in `OwnerAware` (SysUser, SysDept)

### Security

- **JWT** (jjwt 0.13) — `JwtTokenUtil` generates/validates RSA-signed JWTs with configurable expiry
- **Spring Security** — Stateless session, CORS wide-open, public endpoints: `/auth/login`, `/auth/register`, `/auth/info`, `/auth/secret`, `/openapi*`, `/__test`
- **Password** — `DelegatingPasswordEncoder` with BCrypt (strength 12) and SCrypt (default)
- **Algorithm negotiation** — Client sends `Accept-Algorithm` header; default from config (`security.default-algorithm`)
- **Tenant ID generator config** — `security.tenant-id.machine-id` (int, machine ID, 0-1023) and `security.tenant-id.time-mask` (hex string, seed for Feistel round key derivation); consumed by `TenantIdConfig` at startup

### Dev Infrastructure

- `compose.yaml` starts PostgreSQL and Redis
- application.yaml defaults to `dev` profile
- JWT secret and AES keys in application.yaml (change for production)
