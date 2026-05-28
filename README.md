# Onion

基于 DDD 洋葱架构（Onion Architecture）的 Spring Boot 3 后端服务，面向多租户 SaaS 场景，提供身份管理（Identity）、认证授权、数据权限等核心能力。

## 技术栈

| 领域 | 技术选型 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5.11 |
| ORM | [Jimmer](https://babyfish-ct.github.io/jimmer/) — 编译期 APT 代码生成，字段级 Fetcher 加载 |
| 数据库 | PostgreSQL 15+ |
| 缓存 | Caffeine（L1）+ Redis（L2），Redisson 分布式锁 |
| 安全 | Spring Security + JWT（RSA 签名）+ BCrypt/SCrypt 密码编码 |
| 加密 | AES/RSA 请求体加解密，可协商算法 |
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
└── business-svc/                   # 主服务模块
    ├── pom.xml
    └── src/main/java/org/ninng/businesssvc/
        ├── BusinessSvcApplication.java
        ├── identity/               # 身份管理（洋葱架构）
        │   ├── interfaces/rest/    #   REST 控制器
        │   ├── application/        #   应用服务（用例编排）
        │   ├── domain/
        │   │   ├── model/          #   Jimmer 实体定义
        │   │   ├── port/           #   端口接口（领域契约）
        │   │   ├── service/        #   领域服务（纯业务逻辑）
        │   │   └── type/           #   值对象 / 枚举
        │   └── infrastructure/     #   端口实现（适配器）
        ├── common/                 # 跨模块共享契约
        ├── security/               # 请求加解密（AES/RSA）
        ├── component/              # JWT、i18n 等通用组件
        ├── config/                 # Spring 配置
        ├── filter/                 # OncePerRequestFilter 链
        ├── handler/                # 全局异常处理
        ├── context/                # ThreadLocal 上下文持有者
        ├── event/                  # 缓存失效事件
        ├── constant/               # 全局常量
        └── entity/                 # POJO DTO
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

### 请求管线

```
Request → Filter 链（TraceId → SecurityContext → UserContext）
        → AuthenticationFilter（JWT 校验 + 租户 ID 提取）
        → SecureRequestAdvice（请求解密）
        → ApiVersionInterceptor（API 版本路由）
        → Controller → ApplicationService → Port → PortImpl
        → SecureResponseAdvice（响应加密）
        → GlobalExceptionHandler
```

### 多租户

`UserContextHolder` 提供**双层上下文模型**：

- **Real Context**：认证时设置，代表实际登录用户，用于审计追踪
- **Shadow Context**：父租户代理场景 —— 管理员临时以子租户用户身份操作

每个请求**必须**携带 `X-Tenant-Id` 请求头。

### API 版本控制

`@ApiVersion` 注解配合 `ApiVersionHandlerMapping`，根据版本请求头路由到不同 Controller，允许多版本 API 共存。

### 请求加密

`SecureRequestAdvice` / `SecureResponseAdvice` 提供请求体加解密，支持 AES、RSA 等算法。客户端通过 `Accept-Algorithm` 请求头协商算法，`@Security(enabled=false)` 可跳过解密。

## 领域模型

| 实体 | 主键 | 说明 |
|---|---|---|
| **SysUser** | Long（Snowflake） | 用户，M2M 关联角色 |
| **SysRole** | Long（Snowflake） | 角色，system/tenant 类型，数据权限范围 1-6 |
| **SysTenant** | String（16 位 Base32） | 租户，Feistel 置换生成 |
| **SysDept** | Long（Snowflake） | 部门，层级结构 + 闭包表 |
| **SysDeptClosure** | — | 部门闭包表，支持高效子树查询 |
| **SysRoleIdScope** | — | 角色-数据范围关联 |

所有实体混入 `CreatedAware`、`UpdatedAware`、`StatusAware`、`TenantAware`；SysUser/SysDept 额外混入 `OwnerAware`。

## 缓存

两级架构：**Caffeine（L1，20min TTL）→ Redis（L2，1 天 TTL）**

跨实例失效流程：
```
数据变更 → 驱逐本地 L1+L2 → 发布 Redis 消息（携带 instanceId）
        → 所有实例收到 → 对比 instanceId，跳过本地已处理的实例
        → 其他实例驱逐 L1+L2
```

```java
// 声明式缓存
@Cacheable(cacheNames = CacheConstant.USER, key = "#username")

// 批量驱逐
cacheEventPublisher.batch()
    .evict(CacheConstant.USER, username)
    .evict(CacheConstant.USER_ROLE, userId)
    .clear(CacheConstant.USER)
    .publish();
```

## 安全

- **认证**：JWT（jjwt 0.13），RSA 签名，可配置过期时间
- **密码**：`DelegatingPasswordEncoder`，支持 BCrypt（strength 12）+ SCrypt（默认）
- **会话**：无状态，CORS 全开
- **公开端点**：`/auth/**`、`/openapi*`、`/__test`

## License

MIT
