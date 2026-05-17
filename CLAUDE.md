# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the entire project (root + business-svc module)
./mvnw clean install -DskipTests

# Run the business-svc module
./mvnw -pl business-svc spring-boot:run

# Run tests in business-svc module
./mvnw -pl business-svc test

# Run a single test class
./mvnw -pl business-svc test -Dtest=BusinessSvcApplicationTests

# Start dev infrastructure (PostgreSQL + Redis)
docker compose -f business-svc/compose.yaml up -d
```

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
config/        → Spring @Configuration: Security, Jimmer cache, Jackson, Redis cache, i18n
filter/        → OncePerRequestFilter chain (trace ID, user context, security context, etc.)
handler/       → @RestControllerAdvice global exception handler
context/       → ThreadLocal holders (UserContext, SecurityContext, LinkContext, SpringContext)
constant/      → App-wide constants (cache keys, HTTP headers, date formats)
entity/        → POJO DTOs for request/response (R<T>, LoginResp, PageReq, etc.)
```

### Key Architectural Patterns

**1. Unified Response `R<T>`** — All controllers return `R<T>` with `code`, `msg`, `data`, and optional `algorithm` fields. Wraps success/fail/err.

**2. Jimmer ORM** — Entities are Java interfaces with `@Entity`. Keys: `@Id` with `UserIdGenerator` implementations (`TenantIdGenerator` for 16-char Base32 strings, `SnowflakeIdGenerator` for Long Snowflake IDs), `@ManyToMany` with `@JoinTable`, mixin interfaces (`CreatedAware`, `UpdatedAware`, `TenantAware`, `OwnerAware`, etc.). Jimmer DTO language (`.dto` files) generates views, inputs, and specifications.

**3. Jimmer Fetchers** — Field-level fetch control via `SysUserFetcher.$.allScalarFields().password(false).roles(...)`. Combined with `@FetchBy` on controller method return types for runtime fetch-plan enforcement.

**4. Two-Tier Caching** — Jimmer `ChainCacheBuilder` chains local Caffeine cache (L1) with Redis (L2) for ORM entity caching. Spring `@Cacheable` / `CacheManager` via Redis for manual caching. Cache key prefix: `onion:...`.

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
- **SysRole** — id (Long, Snowflake), RBAC roles with `code` and `scope` (data range)
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
