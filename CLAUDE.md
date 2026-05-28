# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build / Run / Test

```bash
# Build & test (from business-svc directory)
./mvnw compile          # Compile (includes Jimmer APT code generation)
./mvnw test             # Run all tests
./mvnw test -Dtest=ClassName   # Run a single test class
./mvnw spring-boot:run         # Start the application (dev profile by default)
./mvnw clean package           # Package for production

# Dev infrastructure (PostgreSQL + Redis)
docker compose -f compose.yaml up -d
```

There is no Maven wrapper at the root — all build commands should be run from `business-svc/`.

## Architecture

### Module layout

The root `pom.xml` is a minimal parent POM (Java 21). The actual application lives in `business-svc/`, a **Spring Boot 3.5.11** service.

### Onion Architecture (identity bounded context)

The `identity` package follows DDD onion/clean architecture with strict layer separation:

```
interfaces/rest/  ──►  application/  ──►  domain/port/ (interface)
                                                 │
                                          infrastructure/ (impl)
```

- **`identity/domain/model/`** — Jimmer entity definitions (`SysUser`, `SysRole`, `SysDept`, `SysTenant`, `SysDeptClosure`, `SysRoleIdScope`)
- **`identity/domain/port/`** — Port interfaces (`UserPort`, `RolePort`, `DeptPort`, `TenantPort`, `UserRolePort`, etc.). Each exposes data operations the application layer needs.
- **`identity/domain/type/`** — Value objects (`RoleType`, `IdType`, `DataScope` sealed interface)
- **`identity/domain/service/`** — Domain services (`RoleScopeAuthorizationService`, `UserQueryPortImpl`, `RoleCheckerPortImpl`)
- **`identity/application/`** — Application services coordinating business logic across ports
- **`identity/infrastructure/`** — Port implementations (adapters), typically extending `CommonRepository`
- **`identity/interfaces/rest/`** — REST controllers

**Dependency rule:** `interfaces → application → domain/port ← infrastructure`. Ports belong to the domain layer (inner) defining "what the domain needs"; infrastructure belongs to the adapter layer (outer) implementing ports with specific technology. Application depends only on port interfaces, never on infrastructure.

Controllers **never** inject repositories directly. All data access goes through `ApplicationService → Port → PortImpl`.

### Common Module (`common/`)

Cross-module shared contracts: `CheckerPort`, `UserQueryPort`, `PageReq`, `R<T>`, `DataScopeType` sealed interface, entity mixin interfaces (`CreatedAware`, etc.).

### Business Layer (non-identity packages)

```
security/      → Request encryption/decryption (AES/RSA/HMAC), AlgorithmHandler strategy pattern
utils/         → ID generators: TenantIdGenerator (16-char Base32, Feistel permutation), SnowflakeIdGenerator
component/     → JWT (jjwt 0.13, RSA signing), i18n (MessageSource + Accept-Language)
config/        → Spring @Config: Security, Jimmer cache, Jackson, CaffeineRedis, Redis Pub/Sub
filter/        → OncePerRequestFilter chain: trace id → user context → security context
handler/       → @RestControllerAdvice global exception handling
context/       → TransmittableThreadLocal holders (UserContext, SecurityContext, LinkContext)
constant/      → Global constants (cache keys, HTTP headers, date formats)
event/         → Cache invalidation events (Local/Redis sealed class) + Spring Event pub/sub
entity/        → POJO DTOs: R<T>, LoginResp, PageReq
```

### Technology stack

| Concern | Implementation |
|---|---|
| ORM | **Jimmer** (`jimmer-spring-boot-starter`) with APT compile-time code generation. Entities define fetchers, views, and DTOs. `@EnableImplicitApi` generates OpenAPI spec at `/openapi.yml` and UI at `/openapi.html`. |
| DB | PostgreSQL 15+ (HikariCP connection pool) |
| Cache L2 | Redis via **Redisson** (not Spring Data Redis for cache ops). Lettuce for connection pooling. |
| Cache L1 | Caffeine (optional local cache) |
| Custom business cache | `cache/` package — `CacheOps` facade with `CacheDomain` definitions, `CacheTypeStrategy` (Value/List/Set/Hash), `CacheLoader` with `@ForCache` auto-registration, Redisson RLock for refresh coordination |
| Security | Spring Security with JWT (jjwt 0.13.0) + BCrypt/SCrypt `DelegatingPasswordEncoder` + AES/RSA request encryption |
| Concurrency | `TransmittableThreadLocal` (Alibaba TTL) for context propagation across threads |

### Request pipeline

```
Request → MainOnceFilterHandler → RequestIdOnceFilterHandler
        → SecurityContextOnceFilterHandler → UserContextOnceFilterHandler
        → AuthenticationFilter (JWT validation)
        → SecureRequestAdvice (decrypt body)
        → ApiVersionInterceptor (version routing)
        → Controller → ApplicationService → Port → PortImpl (CommonRepository)
        → SecureResponseAdvice (encrypt body)
        → GlobalExceptionHandler
        → UserContextOnceFilterHandler.after() (cleanup ThreadLocal)
```

`MainOnceFilterHandler` collects all `OnceFilterHandler` beans, executing `before()` → filter chain → finally `after()` in order.

### Multi-tenancy & user context

`UserContextHolder` provides a **two-layer context model** backed by `TransmittableThreadLocal`:

- **Real context**: Set during authentication, represents the actual logged-in user. Used for audit trails.
- **Shadow context**: For parent-tenant proxy scenarios — a parent admin temporarily operates as a child tenant user. When `isShadow()` is true, all getters return shadow values. Real values are accessible via `getReal*()` methods.

`UserContextHolder.snapshot()` / `restore()` captures and restores context for async tasks.

Every request **must** carry an `X-Tenant-Id` header — enforced by `AuthenticationFilter`.

### Jimmer entity conventions

- **`entity/`** — Request/response DTOs (not ORM entities). `R<T>` is the unified response wrapper with `code`, `msg`, `data`.
- **`model/`** — Jimmer entity definitions (`@Entity`, `@Id` with `UserIdGenerator`, `@ManyToMany` + `@JoinTable`) with associated filter interceptors and trait interfaces (`TenantAware`, `OwnerAware`, `CreatedAware`, `UpdatedAware`, `StatusAware`).
- **`model/filter/`** — Draft interceptors and filters for multi-tenancy, data scope, and owner filtering applied automatically by Jimmer.
- **`CommonRepository`** — Abstract base for all repositories. Provides soft-delete (`withUpdated()`), typed queries, paginated selects, and data permission verification stubs. All Port implementations extend this.
- **Fetchers** — Control field-level loading (e.g., `SysUserFetcher.$.allScalarFields().password(false)`). `@FetchBy` enforces at runtime.
- **`.dto` files** — Generate views, inputs, and specifications from entity definitions.

### API versioning

`@ApiVersion` annotation on controllers combined with `ApiVersionHandlerMapping` routes requests based on a version header, allowing multiple API versions to coexist.

### Encryption

`security/` package provides request/response body encryption via `SecureRequestAdvice` and `SecureResponseAdvice`. Supported algorithms (AES, RSA) are managed by `AlgorithmHandlerFactory`. The default algorithm is configured via `security.default-algorithm`. Clients negotiate encryption via `Accept-Algorithm` header. `@Security(enabled=false)` skips decryption on specific endpoints.

### i18n

`I18nUtil` wraps `MessageSource`. Locale resolved from `Accept-Language` header via `RequestLocaleResolver`. Resource files at `resources/i18n/messages*.properties`.

## Caching

**Dual-level architecture:** Caffeine (L1, 20min TTL) → Redis (L2, 1 day TTL).

Cache names: `USER`=`"u"` (user by username), `USER_ROLE`=`"ur"` (roles by userId). Key prefix: `"onion"`. Redis channel: `"onion:cache:invalidate"`.

**Declarative caching:**
```java
@Cacheable(cacheNames = CacheConstant.USER, key = "#username")
```

**Single eviction:**
```java
cacheEventPublisher.evict(CacheConstant.USER, username);
```

**Batch eviction:**
```java
cacheEventPublisher.batch()
    .evict(CacheConstant.USER, username)
    .evict(CacheConstant.USER_ROLE, userId)
    .clear(CacheConstant.USER)
    .publish();
```

**Cross-instance invalidation flow:** mutation → evict local L1+L2 → publish Redis message (with `instanceId`) → all instances receive → skip if `sourceInstanceId == localInstanceId` (already handled locally), otherwise evict L1+L2. Enable via `onion.cache.invalidation.redis.enabled=true`.

## Domain Model

- **SysUser** — id (Long, Snowflake), name, nickname, password, roles (M2M via user_role_mapping)
- **SysRole** — id (Long, Snowflake), code, roleType (system/tenant), dataScope (1-6), scopeDeptId, remark
- **SysTenant** — id (String, 16-char Base32 TenantIdGenerator), code, name
- **SysDept** — id (Long, Snowflake), department hierarchy, ownerUser, closure table (SysDeptClosure)
- **SysRoleIdScope** — role-scope association for data permission
- All entities mixin `CreatedAware`, `UpdatedAware`, `StatusAware`, `TenantAware`; SysUser/SysDept additionally mixin `OwnerAware`

## Security

- **JWT** (jjwt 0.13) — RSA signed, configurable expiration
- **Spring Security** — Stateless sessions, CORS wide open, public endpoints: `/auth/**`, `/openapi*`, `/__test`
- **Passwords** — `DelegatingPasswordEncoder` with BCrypt (strength 12) + SCrypt (default)
- **ID generator config** — `security.tenant-id.machine-id` (0-1023), `security.tenant-id.time-mask` (hex, Feistel seed)

## Dev Infrastructure

- `compose.yaml`: PostgreSQL + Redis
- Default profile: `dev`
- Keys stored in application.yaml (replace for production)
