# Onion

基于 DDD 洋葱架构（Onion Architecture）的 Spring Boot 3 后端服务，面向多租户 SaaS 场景，提供身份管理（Identity）、认证授权、数据权限、业务缓存等核心能力。

## 技术栈

| 领域 | 技术选型 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.11 |
| ORM | [Jimmer](https://babyfish-ct.github.io/jimmer/) 0.9.120 — 编译期 APT 代码生成，接口式实体定义，字段级 Fetcher 加载，DTO 语言代码生成 |
| 数据库 | PostgreSQL 15+ |
| 缓存 | Caffeine（L1）+ Redis（L2），Redisson 分布式锁，自有业务缓存框架（CacheDomain + CacheOps + CacheLoader） |
| 安全 | Spring Security + JWT（jjwt 0.13，RSA 签名）+ BCrypt/SCrypt 密码编码 |
| 加密 | AES/RSA 请求体加解密，可协商算法 |
| 并发 | Alibaba TransmittableThreadLocal 2.14.5 上下文传递 |
| 构建 | Maven |

## 快速开始

### 环境准备

- JDK 21+
- Docker（用于启动 PostgreSQL + Redis）

### 启动开发基础设施

```bash
docker compose -f compose.yaml up -d
```

### 编译与运行

```bash
cd business-svc

# 编译（含 Jimmer APT 代码生成）
./mvnw compile

# 运行（默认 dev profile）
./mvnw spring-boot:run

# 运行测试
./mvnw test

# 打包
./mvnw clean package
```

> **Windows PowerShell 用户：** 使用 `.\mvnw.cmd` 替代 `./mvnw`。

应用默认启动在 `http://localhost:8080`。

### OpenAPI 文档

启动后访问：
- Swagger UI：`http://localhost:8080/openapi.html`
- OpenAPI Spec：`http://localhost:8080/openapi.yml`

## 项目结构

```
onion/
├── pom.xml                         # 根 POM（Java 21 声明）
├── compose.yaml                    # 开发环境 Docker Compose
├── CLAUDE.md                       # Claude Code 开发指引
└── business-svc/                   # 主服务模块
    ├── pom.xml
    ├── src/main/dto/               # Jimmer DTO 定义（view/input/specification）
    │   ├── User.dto
    │   ├── Role.dto
    │   ├── Dept.dto
    │   └── Tenant.dto
    └── src/main/java/org/ninng/businesssvc/
        ├── BusinessSvcApplication.java
        ├── identity/               # 身份管理（洋葱架构）
        │   ├── interfaces/rest/    #   REST 控制器
        │   ├── application/        #   应用服务（用例编排）
        │   │   └── dto/            #   Jimmer 生成的 DTO 类
        │   ├── domain/
        │   │   ├── model/          #   Jimmer 接口实体定义
        │   │   ├── port/           #   端口接口（领域契约）
        │   │   ├── service/        #   领域服务 + CheckerPort 实现
        │   │   └── type/           #   值对象 / sealed 接口
        │   └── infrastructure/     #   端口实现（继承 CommonRepository）
        ├── common/                 # 跨模块共享契约
        │   └── domain/port/        #   UserCheckerPort, RoleCheckerPort, UserQueryPort
        ├── cache/                  # 业务缓存框架
        │   ├── domain/             #   CacheDomain、KeySpec 定义
        │   ├── ops/                #   CacheOps 主门面
        │   ├── strategy/           #   Value/List/Set/Hash 策略
        │   ├── loader/             #   CacheLoader 接口与注册中心
        │   └── lock/               #   Redisson 分布式锁刷新协调
        ├── security/               # 请求加解密（AES/RSA/HMAC）
        ├── component/              # JWT、i18n 等通用组件
        ├── config/                 # Spring 配置
        ├── filter/                 # OncePerRequestFilter 链
        ├── handler/                # 全局异常处理
        ├── context/                # TransmittableThreadLocal 上下文持有者
        ├── event/                  # 缓存失效事件
        ├── constant/               # 全局常量
        ├── entity/                 # POJO DTO（R、PageReq、异常类）
        ├── repository/             # CommonRepository 抽象基类
        ├── model/                  # Jimmer 过滤器与实体 trait 定义
        ├── utils/                  # 工具类（CollectionDiffUtils、IdUtils）
        └── version/                # API 版本路由
```

## 架构

### 洋葱架构（Identity Bounded Context）

```
interfaces/rest  ──►  application  ──►  domain/port（接口）
                                              │
                                       infrastructure（实现）
```

**依赖规则：** `interfaces → application → domain/port ← infrastructure`

- **端口属于领域层**，定义"领域需要什么"，不关心实现
- **基础设施层实现端口**，接入具体技术（数据库、缓存等）
- Application 层**只依赖端口接口**，对基础设施完全无感知
- 控制器**绝不直接注入 Repository**，所有数据访问经 `ApplicationService → Port → PortImpl`

### 请求管线

```
Request → MainOnceFilterHandler（ContentCachingRequestWrapper 包裹请求体缓存）
        → RequestIdOnceFilterHandler（TraceId 注入 MDC）
        → SecurityContextOnceFilterHandler → UserContextOnceFilterHandler
        → AuthenticationFilter（JWT 校验 + 租户 ID 提取）
        → SecureRequestAdvice（请求解密）
        → ApiVersionInterceptor（API 版本路由）
        → Controller → ApplicationService → Port → PortImpl
        → SecureResponseAdvice（响应加密）
        → GlobalExceptionHandler（统一日志 + 响应封装，异常时可读取缓存的请求体）
        → UserContextOnceFilterHandler.after()（清理 ThreadLocal）
```

### 多租户

`UserContextHolder` 提供**双层上下文模型**：

- **Real Context**：认证时设置，代表实际登录用户，用于审计追踪
- **Shadow Context**：父租户代理场景 —— 管理员临时以子租户用户身份操作

每个请求**必须**携带 `X-Tenant-Id` 请求头。

**上下文模式：** `UserContextHolder.withMode(UserContextMode.DisabledType, () -> ...)` 可临时绕过租户/数据过滤，用于跨租户查询等场景。

### 密封接口模式（Java 17+）

代码库中使用了两个典型的 `sealed interface` 模式：

1. **DataScope 枚举 + DataScopeType 密封接口** — 6 种数据范围（仅本人/本部门/本部门及子部门/指定人/指定部门/全租户）通过 `toType()` 映射为类型安全的策略分发
2. **UserContextMode 密封接口** — 仅允许 `DefaultType` 和 `DisabledType`，控制是否启用数据过滤

### API 版本控制

`@ApiVersion` 注解配合 `ApiVersionHandlerMapping`，根据版本请求头路由到不同 Controller，允许多版本 API 共存。

### 请求加密

`SecureRequestAdvice` / `SecureResponseAdvice` 提供请求体加解密，支持 AES、RSA 等算法。客户端通过 `Accept-Algorithm` 请求头协商算法，`@Security(enabled=false)` 可跳过解密。

## 领域模型

| 实体 | 主键 | 说明 |
|---|---|---|
| **SysUser** | Long（Snowflake） | 用户，M2M 关联角色，混入 CreatedAware/UpdatedAware/StatusAware/OwnerAware/TenantAware |
| **SysRole** | Long（Snowflake） | 角色，system/tenant 类型，数据权限范围 1-6，1:N 关联 SysRoleIdScope |
| **SysTenant** | String（16 位 Base32） | 租户，Feistel 置换生成 |
| **SysDept** | Long（Snowflake） | 部门，层级结构 + 闭包表 |
| **SysDeptClosure** | — | 部门闭包表，支持高效子树查询 |
| **SysRoleIdScope** | — | 角色-数据范围关联 |

**实体约定：** Jimmer 实体定义为 **Java 接口**，`@Entity` + `@Id` + `@GeneratedValue`。全部混入 `CreatedAware`、`UpdatedAware`、`StatusAware`、`TenantAware`；SysUser/SysDept 额外混入 `OwnerAware`。密码字段默认不加载（通过 Fetcher 排除）。

### Jimmer DTO 语言

通过 `src/main/dto/*.dto` 文件定义视图、输入和规格，编译期自动生成：

```
export org...SysUser → package org...dto

UserDetailsView { id name password tenantId ownerDeptId }
UserSelectionView { id as value name as label }

input UserUpdateInput { id dynamic nickname? id(roles) as roleIds }
specification UserSpecification { name like/^(nickname) }
```

### Jimmer 自动过滤

- **TenantFilter** — 全局 SQL 过滤器，自动追加 `tenantId = ?`
- **TenantDraftInterceptor** — 写入时自动设 `tenantId`，更新时校验归属
- **CreatedDraftInterceptor / UpdatedDraftPreProcessor** — 自动填充审计字段

## 缓存

两级架构：**Caffeine（L1，20min TTL）→ Redis（L2，1 天 TTL）**

### Spring 声明式缓存

```java
@Cacheable(cacheNames = CacheConstant.USER, key = "#username")
```

### 业务缓存框架

项目实现了独立的业务缓存框架（`cache/` 包），适用于复杂缓存场景：

| 组件 | 职责 |
|---|---|
| **CacheDomain** | 缓存域定义（名称/类型/键模式/租户与ID需求声明） |
| **CacheOps** | 主门面：get/batchGet/evict/clear/refresh/list/hashGet |
| **CacheLoader** | 缓存加载器接口：load/batchLoad/loadPage，按域名自动注册 |
| **CacheLoaderRegistry** | 加载器注册中心，域名→加载器映射 |
| **CacheTypeStrategy** | Value/List/Set/Hash 四种存储策略 |
| **RefreshStrategy** | 刷新策略：fixed(pageSize) 分页刷新，Redisson 锁协调 |

```java
// 编程式缓存操作
cacheOps.get(domain, tenantId, id);
cacheOps.batchGet(domain, tenantId, ids);
cacheOps.evict(domain, tenantId, id);
cacheOps.refresh(domain, tenantId, RefreshStrategy.fixed(3));
```

跨实例失效流程：数据变更 → 驱逐本地 L1+L2 → 发布 Redis 消息（携带 `instanceId`）→ 其他实例接收 → 跳过本地源实例 → 驱逐 L1+L2。

```java
cacheEventPublisher.batch()
    .evict(CacheConstant.USER, username)
    .evict(CacheConstant.USER_ROLE, userId)
    .clear(CacheConstant.USER)
    .publish();
```

## CheckerPort 跨模块校验

在 `common/domain/port/` 中定义校验接口，由 `identity/domain/service/` 实现，实现跨模块的可见性校验：

- **UserCheckerPort** — `checkVisible(userId)` 校验目标用户对当前操作者可见
- **RoleCheckerPort** — `checkVisible(roleIds)` 校验目标角色对当前操作者可见

应用服务中通过 `CollectionDiffUtils.diff(oldIds, newIds).changed()` 计算变更集，仅对变更部分执行校验，避免全量查询。

## 错误处理

- **异常层次：** `BizException` → `ServiceException | PermissionsException | SecurityException`
- **错误码：** `ErrCode` 枚举定义统一错误码
- **响应体：** `R<T>` 包装 `(code, msg, data, algorithm, traceId)`
- **全局处理：** `GlobalExceptionHandler`（`@RestControllerAdvice`）统一 `log.error` 级别输出完整异常栈
- **日志上下文：** 每条异常日志包含 `traceId`、`uri`、`method`、`tenantId`、`userId`、请求参数（query/form）、请求体（`ContentCachingRequestWrapper` 缓存，截断 2000 字符，换行展平）
- **敏感参数脱敏：** `password`、`secret`、`token` 等参数值自动替换为 `***`

## 通用工具

- **CollectionDiffUtils** — 集合差异比对工具，支持 `equals`、Key 提取函数、Comparator 三种策略，返回 `DiffResult<T>(add, del)` 记录 + `changed()` 合并视图
- **IdUtils** — 租户 ID 生成（16 位 Base32，Feistel 置换）与雪花 ID

## 安全

- **认证**：JWT（jjwt 0.13），RSA 签名，可配置过期时间
- **密码**：`DelegatingPasswordEncoder`，支持 BCrypt（strength 12）+ SCrypt（默认）
- **会话**：无状态，CORS 全开
- **公开端点**：`/auth/**`、`/openapi*`、`/__test`

## License

MIT
