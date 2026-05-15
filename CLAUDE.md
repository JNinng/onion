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

**2. Jimmer ORM** — Entities are Java interfaces with `@Entity`. Keys: `@Id` with `UUIDIdGenerator`, `@ManyToMany` with `@JoinTable`, mixin interfaces (`CreatedAware`, `UpdatedAware`, `TenantAware`, etc.). Jimmer DTO language (`.dto` files) generates views, inputs, and specifications.

**3. Jimmer Fetchers** — Field-level fetch control via `SysUserFetcher.$.allScalarFields().password(false).roles(...)`. Combined with `@FetchBy` on controller method return types for runtime fetch-plan enforcement.

**4. Two-Tier Caching** — Jimmer `ChainCacheBuilder` chains local Caffeine cache (L1) with Redis (L2) for ORM entity caching. Spring `@Cacheable` / `CacheManager` via Redis for manual caching. Cache key prefix: `onion:...`.

**5. Filter Chain Pattern** — `MainOnceFilterHandler` collects all `OnceFilterHandler` beans and runs `before()` before the filter chain, `after()` in the finally block. Implementations: `RequestIdOnceFilterHandler` (trace ID), `SecurityContextOnceFilterHandler` (encryption algorithm), `UserContextOnceFilterHandler` (cleanup).

**6. Request Encryption** — `SecureRequestAdvice` / `SecureResponseAdvice` via `@ControllerAdvice`. Strategy pattern via `AlgorithmHandler` interface (implementations: NONE, AES, etc.). `@Security(enabled=false)` annotation skips decryption. Supports client-negotiated algorithm via `Accept-Algorithm` header.

**7. Context Holders** — `UserContextHolder`, `SecurityContextHolder`, `LinkContextHolder` manage per-request state using `TransmittableThreadLocal`. Spring Boot default profile is `dev`.

**8. i18n** — `I18nUtil` wraps Spring `MessageSource`. Locale resolved via `Accept-Language` header (`RequestLocaleResolver`). Properties files at `resources/i18n/messages*.properties`.

### Domain Model

- **SysUser** — id (UUID), name, nickname, password, roles (M2M via user_role_mapping)
- **SysRole** — RBAC roles
- **SysTenant** — Multi-tenant support with `code` and `name`
- **SysDept** — Department hierarchy
- All entities mix in `CreatedAware`, `UpdatedAware`, `StatusAware`, `TenantAware`

### Security

- **JWT** (jjwt 0.13) — `JwtTokenUtil` generates/validates RSA-signed JWTs with configurable expiry
- **Spring Security** — Stateless session, CORS wide-open, public endpoints: `/auth/login`, `/auth/register`, `/auth/info`, `/auth/secret`, `/openapi*`, `/__test`
- **Password** — `DelegatingPasswordEncoder` with BCrypt (strength 12) and SCrypt (default)
- **Algorithm negotiation** — Client sends `Accept-Algorithm` header; default from config (`security.default-algorithm`)

### Dev Infrastructure

- `compose.yaml` starts PostgreSQL and Redis
- application.yaml defaults to `dev` profile
- JWT secret and AES keys in application.yaml (change for production)
