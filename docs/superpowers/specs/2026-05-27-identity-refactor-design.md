# Move User/Dept/Tenant into Identity Bounded Context

## Motivation

Consolidate user, department, and tenant domain logic into the `identity` bounded context, which already follows a DDD-style onion architecture with Port/Adapter pattern (as pioneered by `Role`).

## Scope

Move the **business layers only**: entity models, repositories, services, controllers, and DTO definitions. Infrastructure cross-cutting concerns (filters, context holders, config, common base classes) remain in place.

## Mapping

| Current Location | Target Identity Package | Pattern |
|---|---|---|
| `model/SysUser.java` | `identity/domain/model/SysUser.java` | Entity |
| `model/SysDept.java` | `identity/domain/model/SysDept.java` | Entity |
| `model/SysDeptClosure.java` | `identity/domain/model/SysDeptClosure.java` | Entity |
| `model/SysTenant.java` | `identity/domain/model/SysTenant.java` | Entity |
| `model/common/TenantAware.java` | `identity/domain/model/TenantAware.java` | Entity mixin |
| *(new)* | `identity/domain/port/UserPort.java` | Port interface |
| *(new)* | `identity/domain/port/UserRolePort.java` | Port interface |
| *(new)* | `identity/domain/port/DeptPort.java` | Port interface |
| *(new)* | `identity/domain/port/TenantPort.java` | Port interface |
| `repository/UserRepository.java` | `identity/infrastructure/UserPortImpl.java` | Port implementation |
| `repository/UserRoleRepository.java` | `identity/infrastructure/UserRolePortImpl.java` | Port implementation |
| `repository/DeptRepository.java` | `identity/infrastructure/DeptPortImpl.java` | Port implementation |
| `repository/TenantRepository.java` | `identity/infrastructure/TenantPortImpl.java` | Port implementation |
| `service/UserService.java` | `identity/service/UserApplicationService.java` | Application service |
| `service/DeptService.java` | `identity/service/DeptApplicationService.java` | Application service |
| `service/TenantService.java` | `identity/service/TenantApplicationService.java` | Application service |
| `controller/UserController.java` | `identity/rest/UserController.java` | REST controller |
| `controller/DeptController.java` | `identity/rest/DeptController.java` | REST controller |
| `controller/TenantController.java` | `identity/rest/TenantController.java` | REST controller |
| `src/main/dto/User.dto` | target: `identity/application/dto/` | Jimmer DTO |
| `src/main/dto/Dept.dto` | target: `identity/application/dto/` | Jimmer DTO |
| `src/main/dto/Tenant.dto` | target: `identity/application/dto/` | Jimmer DTO |

### Updated Java package for DTO files

Each `.dto` file changes its `export` and `-> package`:

- `export org.ninng.businesssvc.identity.domain.model.SysUser` → `-> package org.ninng.businesssvc.identity.application.dto`
- Same pattern for Dept and Tenant.

### What stays unchanged

| Artifact | Reason |
|---|---|
| `repository/CommonRepository` | Shared base class for all PortImpls |
| `utils/TenantIdGenerator` | Utility, not domain logic |
| `config/TenantIdConfig` | Configuration, not domain logic |
| `model/filter/TenantFilter`, `TenantScope`, `TenantDraftInterceptor` | Cross-cutting infrastructure |
| `context/UserContextHolder` | Cross-cutting infrastructure |
| `filter/UserContextOnceFilterHandler` | Cross-cutting infrastructure |
| `component/DatabaseUserDetailsService` | Spring Security integration |

## Architecture (per domain)

Following the Role pattern precisely:

```
Controller → ApplicationService → Port (interface) → PortImpl (infrastructure)
                                                      extends CommonRepository
```

### Port responsibilities

Each Port interface exposes the data operations that the application service needs:

- **UserPort**: `findByUsername`, `update`, `register`, `select`
- **UserRolePort**: `findByUserId` (role assignments for a user)
- **DeptPort**: `create`, `list`, `deleteById`
- **TenantPort**: `create`, `list`, `findByCode`, `deleteById`

### Application service responsibilities

Coordinate business logic across port calls. Example: `TenantApplicationService.create()` generates a tenant code, then delegates to `TenantPort.create()`.

### Controller simplification

Controllers no longer inject repositories directly. All data access goes through the application service.

## Updated cross-references

All files outside `identity` that import the moved types must update their import paths:

- `DatabaseUserDetailsService` → imports `SysUser`, `UserDetailsView` → update to `identity.domain.model`
- `UserContextHolder` → imports `SysUser` → update to `identity.domain.model`
- Files referencing `SysDeptClosure`, `TenantAware`, etc. → update imports

## Migration order

1. Create Port interfaces (`domain/port/`)
2. Move and adapt entity models (`domain/model/`)
3. Create Port implementations (`infrastructure/`)
4. Move and adapt application services (`service/`)
5. Move controllers (`rest/`)
6. Update DTO files
7. Update cross-references (imports) in files outside identity
8. Delete old files
9. Verify compilation (`mvnw compile`)
