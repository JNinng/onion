# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build / Run / Test

```bash
# Build & test (from business-svc directory)
./mvnw compile                    # Compile (includes Jimmer APT code generation)
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run a single test class
./mvnw spring-boot:run            # Start the application (dev profile by default)
./mvnw clean package              # Package for production

# Dev infrastructure (PostgreSQL + Redis)
docker compose -f compose.yaml up -d
```

There is no Maven wrapper at the root — all build commands should be run from `business-svc/`.

**Windows/PowerShell note:** Use `.\mvnw.cmd` instead of `./mvnw` in PowerShell. The `./mvnw` syntax doesn't work in PS — use `mvnw.cmd compile`, `mvnw.cmd test`, etc.

**Test conventions:**
- Integration tests use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)` + `@ActiveProfiles("test")`
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` + `@Order(N)` for ordered integration tests
- `@BeforeEach` cleanup of test data (e.g., flushing Redis keys matching a pattern)
- Unit tests in `business-svc/src/test/java/org/ninng/businesssvc/`

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

Cross-module shared contracts in `common/domain/port/`:
- **CheckerPorts** (`UserCheckerPort`, `RoleCheckerPort`) — validation interfaces for cross-domain visibility checks, implemented by domain services in `identity/domain/service/`
- `UserQueryPort` — cross-module user lookups

Other shared types: `PageReq`, `R<T>`, `DataScopeType` sealed interface, entity mixin interfaces (`CreatedAware`, etc.).

### Business Layer (non-identity packages)

```
security/      → Request encryption/decryption (AES/RSA/HMAC), AlgorithmHandler strategy pattern
utils/         → ID generators: TenantIdGenerator (16-char Base32, Feistel permutation), SnowflakeIdGenerator
                 CollectionDiffUtils — DiffResult<T> record (add/del lists + changed() convenience)
component/     → JWT (jjwt 0.13, RSA signing), i18n (MessageSource + Accept-Language)
config/        → Spring @Config: Security, Jimmer cache, Jackson, CaffeineRedis, Redis Pub/Sub
                 BusinessCacheConfig — CacheOps bean (with optional Caffeine L1 via onion.cache.business.local.enabled)
filter/        → OncePerRequestFilter chain: trace id → user context → security context
handler/       → @RestControllerAdvice global exception handling
context/       → TransmittableThreadLocal holders (UserContext, SecurityContext, LinkContext)
                 UserContextMode sealed interface: DefaultType | DisabledType (skips owner/role data filtering)
constant/      → Global constants (cache keys, HTTP headers, date formats, TRACE_ID_LENGTH=32, TENANT_ID_LENGTH=16)
event/         → Cache invalidation events (Local/Redis sealed class) + Spring Event pub/sub
entity/        → POJO DTOs: R<T>, LoginResp, PageReq
                 Exceptions: BizException → ServiceException | PermissionsException | SecurityException (ErrCode enum)
```

### Technology stack

| Concern | Implementation |
|---|---|
| ORM | **Jimmer** (`jimmer-spring-boot-starter` 0.9.120) with APT compile-time code generation. Entities define fetchers, views, and DTOs. `@EnableImplicitApi` generates OpenAPI spec at `/openapi.yml` and UI at `/openapi.html`. |
| DB | PostgreSQL 15+ (HikariCP connection pool) |
| Cache L2 | Redis via **Redisson** (not Spring Data Redis for cache ops). Lettuce for connection pooling. |
| Cache L1 | Caffeine (optional local cache) |
| Custom business cache | `cache/` package — `CacheOps` facade with `CacheDomain` definitions, `CacheTypeStrategy` (Value/List/Set/Hash), `CacheLoader` with auto-registration, Redisson RLock for refresh coordination |
| Security | Spring Security with JWT (jjwt 0.13.0) + BCrypt/SCrypt `DelegatingPasswordEncoder` + AES/RSA request encryption |
| Concurrency | `TransmittableThreadLocal` (Alibaba TTL 2.14.5) for context propagation across threads |

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

**Context mode pattern:** `UserContextHolder.withMode(UserContextMode.DisabledType.INSTANCE, () -> ...)` temporarily disables Jimmer's tenant/owner data filters for operations like cross-tenant queries. `UserContextMode` is a sealed interface with `DefaultType` (filters active) and `DisabledType` (filters bypassed).

### Sealed interface patterns (Java 17+)

Two notable sealed interface patterns in this codebase:

1. **`DataScope` enum + `DataScopeType` sealed interface** — `DataScope` (1-6 ordinal) maps to `PersonalType | DepartmentType | ... | AllTenantType` via `toType()`, enabling type-safe pattern matching for data permission strategy dispatch.

2. **`UserContextMode` sealed interface** — permits only `DefaultType` and `DisabledType`, controlling whether owner/role data filters are applied.

## Jimmer ORM Conventions

### Entity definitions
Jimmer entities are **Java interfaces** (not classes) with property-style methods:
- `@Entity` on interface, `@Id` + `@GeneratedValue(generatorType = SnowflakeIdGenerator.class)` on primary key
- Mixin trait interfaces: `TenantAware`, `OwnerAware`, `CreatedAware`, `UpdatedAware`, `StatusAware`
- `@ManyToMany` + `@JoinTable` for M2M (e.g., SysUser ↔ SysRole via `user_role_mapping`)
- `@OneToMany(mappedBy = "...")` for 1:N (e.g., SysRole → SysRoleIdScope)
- `@EnumType(EnumType.Strategy.ORDINAL)` for enum persistence
- Fields not loaded by default (like `password`) controlled via Fetcher: `.password(false)`

### DTO language (`.dto` files)
Jimmer code generation from `business-svc/src/main/dto/*.dto`:
- `export Entity → package` — points to the entity and target package
- `View { #allScalars }` — all scalar fields view; `{ id name }` — specific fields
- `input EntityInput { id dynamic nickname? }` — updatable input (dynamic = partial update, `?` = optional)
- `specification EntitySpec { name like/^(nickname) }` — query specification with LIKE patterns
- `id(entityProp) as aliasIds` — extract ID collection from association

### Fetchers
Compile-time generated fetchers for field-level loading control:
```java
SysUserFetcher.$.allScalarFields().password(false)
    .roles(SysRoleFetcher.$.allScalarFields())
```
Use `@FetchBy("CONSTANT_NAME")` in controller return types for OpenAPI doc generation.

### CommonRepository
All Port implementations extend `CommonRepository<E, ID>` which extends `AbstractJavaRepository`:
- `getTable()` — returns the Jimmer table definition (must override)
- `withUpdated()` — builds update statement with `updatedAt` + `updatedBy` set
- `delete(ID)` — soft delete via `deletedAt` column
- Multiple `select()` overloads: View-based, Fetcher-based, Specification-based, paginated
- `verifyPermissions()` — data ownership verification stub

## Business Cache Architecture

**Dual-level architecture:** Caffeine (L1, 20min TTL) → Redis (L2, 1 day TTL).

Beyond Spring's `@Cacheable`, the project has a custom **business cache framework**:

- **`CacheDomain`** record — defines a domain: `name`, `type` (Value/List/Set/Hash), `keyPattern`, `KeySpec` (tenant/id/fields requirements)
- **`CacheOps`** — main facade: `get()`, `batchGet()`, `evict()`, `clear()`, `refresh()`, `list()`, `hashGet()`
- **`CacheLoader<ID, TID, V>`** — interface for populating cache on miss: `load(key)`, `loadPage(key, page, pageSize)`, `batchLoad(pattern, ids)`
- **`CacheLoaderRegistry`** — auto-collects all `CacheLoader` beans, maps them to `CacheDomain` by name
- **`CacheDomains`** — registry of all `CacheDomain` beans, enforces unique names
- **`CacheStrategyFactory`** → `ValueStrategy | ListStrategy | SetStrategy | HashStrategy`
- **`RefreshStrategy`** — `fixed(pageSize)` for batched refresh with lock coordination

Cache names: `USER`=`"u"` (user by username), `USER_ROLE`=`"ur"` (roles by userId). Key prefix: `"onion"`. Redis channel: `"onion:cache:invalidate"`.

**Declarative caching:**
```java
@Cacheable(cacheNames = CacheConstant.USER, key = "#username")
```

**Programmatic cache ops:**
```java
cacheOps.get(domain, tid, id);
cacheOps.batchGet(domain, tid, ids);
cacheOps.evict(domain, tid, id);
cacheOps.refresh(domain, tid, RefreshStrategy.fixed(pageSize));
```

**Cross-instance invalidation flow:** mutation → evict local L1+L2 → publish Redis message (with `instanceId`) → all instances receive → skip if `sourceInstanceId == localInstanceId` (already handled locally), otherwise evict L1+L2. Enable via `onion.cache.invalidation.redis.enabled=true`.

## Domain Model

- **SysUser** — id (Long, Snowflake), name, nickname, password, roles (M2M via user_role_mapping)
- **SysRole** — id (Long, Snowflake), code, roleType (system/tenant), dataScope (1-6), scopeDeptId, remark, roleIdScopes (1:N)
- **SysTenant** — id (String, 16-char Base32 TenantIdGenerator), code, name
- **SysDept** — id (Long, Snowflake), department hierarchy, ownerUser, closure table (SysDeptClosure)
- **SysRoleIdScope** — role-scope association for data permission
- All entities mixin `CreatedAware`, `UpdatedAware`, `StatusAware`, `TenantAware`; SysUser/SysDept additionally mixin `OwnerAware`

## Jimmer Filters & Draft Interceptors

- **`TenantFilter`** — global `Filter<TenantAwareProps>`, auto-applies `tenantId = currentUser.tenantId` to all queries
- **`TenantDraftInterceptor`** — auto-sets `tenantId` on insert, validates tenant match on update
- **`CreatedDraftInterceptor` / `UpdatedDraftPreProcessor`** — auto-sets `createdAt`/`createdBy`/`updatedAt`/`updatedBy`
- Filters are **bypassable** via `UserContextHolder.withMode(DisabledType, ...)` for cross-tenant operations

## Security

- **JWT** (jjwt 0.13) — RSA signed, configurable expiration
- **Spring Security** — Stateless sessions, CORS wide open, public endpoints: `/auth/**`, `/openapi*`, `/__test`
- **Passwords** — `DelegatingPasswordEncoder` with BCrypt (strength 12) + SCrypt (default)
- **ID generator config** — `security.tenant-id.machine-id` (0-1023), `security.tenant-id.time-mask` (hex, Feistel seed)
- **Request encryption** — `SecureRequestAdvice`/`SecureResponseAdvice` (AES/RSA), algorithm negotiation via `Accept-Algorithm`, opt-out via `@Security(enabled=false)`

## Error Handling

- `BizException` (base) → `ServiceException` | `PermissionsException` | `SecurityException`
- `ErrCode` enum defines error codes, `R<T>` wraps `(code, msg, data, algorithm, traceId)`
- `GlobalExceptionHandler` (`@RestControllerAdvice`) catches all exceptions

## Git / Commit Convention

```bash
# Commit message format: <type>: <description>
git commit -m "type: 简短描述" -m "详细说明（可选）"
```

Recent commit types: `feat`（新功能）, `fix`（修复）, `refactor`（重构）, `docs`（文档注释）, `test`（测试）, `chore`（杂项）.

## Dev Infrastructure

- `compose.yaml`: PostgreSQL + Redis
- Default profile: `dev`
- Keys stored in application.yaml (replace for production)
