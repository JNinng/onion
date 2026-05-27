# User/Dept/Tenant → Identity 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 User、Dept、Tenant 三个领域的业务代码（实体、仓库、服务、控制器、DTO）移入 identity 包，并统一采用 Port/Adapter 模式。

**Architecture:** Controller → ApplicationService → Port（接口）→ PortImpl（基础设施，继承 CommonRepository）。遵循 Role 的既有模式。

**Tech Stack:** Java 21, Spring Boot, Jimmer ORM, Jimmer DTO

---

### 前置说明

- **TenantAware** 留在 `model/common/`（共享内核，同 CreatedAware/UpdatedAware），identity 实体通过 import 引用
- **CommonRepository** 留在 `repository/`，作为所有 PortImpl 的基类
- 实体移动后，Jimmer 注解处理器会自动在新包下生成 `*Fetcher`、`*Table`、`*Props` 类
- 所有 import 变更通过 `grep -r` 查找旧引用统一处理

### 涉及的所有文件清单

**新建（Port 接口）：**
- `identity/domain/port/UserPort.java`
- `identity/domain/port/UserRolePort.java`
- `identity/domain/port/DeptPort.java`
- `identity/domain/port/TenantPort.java`

**移动（实体）：**
- `model/SysUser.java` → `identity/domain/model/SysUser.java`
- `model/SysDept.java` → `identity/domain/model/SysDept.java`
- `model/SysDeptClosure.java` → `identity/domain/model/SysDeptClosure.java`
- `model/SysTenant.java` → `identity/domain/model/SysTenant.java`

**移动+改编（基础设施）：**
- `repository/UserRepository.java` → `identity/infrastructure/UserPortImpl.java`
- `repository/UserRoleRepository.java` → `identity/infrastructure/UserRolePortImpl.java`
- `repository/DeptRepository.java` → `identity/infrastructure/DeptPortImpl.java`
- `repository/TenantRepository.java` → `identity/infrastructure/TenantPortImpl.java`

**移动+改编（应用服务）：**
- `service/UserService.java` → `identity/service/UserApplicationService.java`
- `service/DeptService.java` → `identity/service/DeptApplicationService.java`
- `service/TenantService.java` → `identity/service/TenantApplicationService.java`

**移动（控制器）：**
- `controller/UserController.java` → `identity/rest/UserController.java`
- `controller/DeptController.java` → `identity/rest/DeptController.java`
- `controller/TenantController.java` → `identity/rest/TenantController.java`

**修改（DTO）：**
- `src/main/dto/User.dto`（更新 export 和 package）
- `src/main/dto/Dept.dto`（更新 export 和 package）
- `src/main/dto/Tenant.dto`（更新 export 和 package）

**更新引用（identity 外部文件）：**
- `component/DatabaseUserDetailsService.java`
- `context/UserContextHolder.java`
- `filter/UserContextOnceFilterHandler.java`
- `model/filter/TenantFilter.java`
- `model/filter/TenantScope.java`
- `model/filter/TenantDraftInterceptor.java`
- `security/*.java`
- `interceptor/*.java`
- 其他通过 grep 发现的所有引用旧包的文件

**删除：**
- `model/SysUser.java`, `model/SysDept.java`, `model/SysDeptClosure.java`, `model/SysTenant.java`
- `repository/UserRepository.java`, `repository/UserRoleRepository.java`, `repository/DeptRepository.java`, `repository/TenantRepository.java`
- `service/UserService.java`, `service/DeptService.java`, `service/TenantService.java`
- `controller/UserController.java`, `controller/DeptController.java`, `controller/TenantController.java`

---

### Task 1: 移动实体到 identity/domain/model/

**Files:**
- Move: `model/SysUser.java` → `identity/domain/model/SysUser.java`
- Move: `model/SysDept.java` → `identity/domain/model/SysDept.java`
- Move: `model/SysDeptClosure.java` → `identity/domain/model/SysDeptClosure.java`
- Move: `model/SysTenant.java` → `identity/domain/model/SysTenant.java`

- [ ] **Step 1: 读取每个实体文件**

Read: `model/SysUser.java`, `model/SysDept.java`, `model/SysDeptClosure.java`, `model/SysTenant.java`

- [ ] **Step 2: 创建目标目录并移动实体，更新 package 声明**

每个实体文件的操作：
1. 将文件写入新路径 `identity/domain/model/{实体}.java`
2. 将 `package org.ninng.businesssvc.model;` 改为 `package org.ninng.businesssvc.identity.domain.model;`
3. 如果引用了 `model.common.TenantAware`，保留 import `org.ninng.businesssvc.model.common.TenantAware`
4. 如果引用了 `model.SysRole`，更新为 `identity.domain.model.SysRole`
5. 如果引用了 `model.SysUser` 或 `model.SysDept` 等自身实体，更新为新路径

例如 SysUser.java 更新 package 并调整跨实体引用：
```java
package org.ninng.businesssvc.identity.domain.model;

// 保留的 import（共享内核）
import org.ninng.businesssvc.model.common.TenantAware;
// Role 已经在 identity 包中
import org.ninng.businesssvc.identity.domain.model.SysRole;
// 同包实体引用保持
```

- [ ] **Step 3: 验证实体完整性**

确保 SysUser 引用 SysDept、SysRole；SysDept 引用 SysUser、SysDeptClosure、TenantAware；SysTenant 引用 TenantAware 等都已正确更新。

- [ ] **Step 4: 提交**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/identity/domain/model/SysUser.java
git add business-svc/src/main/java/org/ninng/businesssvc/identity/domain/model/SysDept.java
git add business-svc/src/main/java/org/ninng/businesssvc/identity/domain/model/SysDeptClosure.java
git add business-svc/src/main/java/org/ninng/businesssvc/identity/domain/model/SysTenant.java
git commit -m "refactor: move SysUser/SysDept/SysDeptClosure/SysTenant entities to identity/domain/model"
```

---

### Task 2: 创建 Port 接口

**Files:**
- Create: `identity/domain/port/UserPort.java`
- Create: `identity/domain/port/UserRolePort.java`
- Create: `identity/domain/port/DeptPort.java`
- Create: `identity/domain/port/TenantPort.java`

- [ ] **Step 1: 创建 UserPort.java**

```java
package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.model.dto.UserSpecification;

import java.util.List;

public interface UserPort {

    @Nullable
    UserDetailsView findByUsername(String username);

    Boolean update(UserUpdateInput input);

    SysUser register(org.babyfish.jimmer.Input<SysUser> input);

    <V extends View<SysUser>> List<V> select(Class<V> viewClass);

    <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification);

    Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification);
}
```

- [ ] **Step 2: 创建 UserRolePort.java**

```java
package org.ninng.businesssvc.identity.domain.port;

import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;

import java.util.List;

public interface UserRolePort {

    List<RoleDetailsView> findByUserId(@Nullable Long userId);

    List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant);
}
```

- [ ] **Step 3: 创建 DeptPort.java**

```java
package org.ninng.businesssvc.identity.domain.port;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.model.dto.DeptSpecification;

public interface DeptPort {

    SysDept create(Fetcher<SysDept> fetcher, org.babyfish.jimmer.Input<SysDept> input);

    Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification);

    Boolean deleteById(Long id);
}
```

- [ ] **Step 4: 创建 TenantPort.java**

```java
package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.model.dto.TenantSpecification;

public interface TenantPort {

    SysTenant create(Fetcher<SysTenant> fetcher, org.babyfish.jimmer.Input<SysTenant> input);

    @Nullable
    SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code);

    Boolean deleteById(String id);

    Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification);
}
```

- [ ] **Step 5: 提交**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/identity/domain/port/
git commit -m "refactor: add UserPort/UserRolePort/DeptPort/TenantPort interfaces"
```

---

### Task 3: 更新 DTO 文件

**Files:**
- Modify: `src/main/dto/User.dto`
- Modify: `src/main/dto/Dept.dto`
- Modify: `src/main/dto/Tenant.dto`

- [ ] **Step 1: 更新 User.dto**

```
export org.ninng.businesssvc.identity.domain.model.SysUser
    -> package org.ninng.businesssvc.identity.application.dto

UserView {
    #allScalars
}

UserDetailsView {
    id
    name
    password
    tenantId
    ownerDeptId
}

UserSelectionView {
    id as value
    name as label
}

specification UserSpecification {
    name
    like/^(nickname)
}

input UserUpdateInput {
    id
    remark
    dynamic tenantId
    dynamic name?
    dynamic nickname?
    dynamic roles? {
        id
    }
}

input LoginInput {
    #allScalars(this)
    -id
    -nickname
}

input RegisterInput {
    #allScalars(this)
    -id
}
```

- [ ] **Step 2: 更新 Dept.dto**

```
export org.ninng.businesssvc.identity.domain.model.SysDept
    -> package org.ninng.businesssvc.identity.application.dto

import com.fasterxml.jackson.annotation.JsonFormat

input DeptCreateInput {
    name
    fixed ownerUserId
    parentId
}

specification DeptSpecification {
    #allScalars
}
```

- [ ] **Step 3: 更新 Tenant.dto**

```
export org.ninng.businesssvc.identity.domain.model.SysTenant
    -> package org.ninng.businesssvc.identity.application.dto

import com.fasterxml.jackson.annotation.JsonFormat

input TenantCreateInput {
    name
}

specification TenantSpecification {
    #allScalars
}
```

- [ ] **Step 4: 提交**

```bash
git add business-svc/src/main/dto/
git commit -m "refactor: update DTO files to target identity domain model and application dto packages"
```

---

### Task 4: 创建 Port 实现（基础设施层）

**Files:**
- Create: `identity/infrastructure/UserPortImpl.java`
- Create: `identity/infrastructure/UserRolePortImpl.java`
- Create: `identity/infrastructure/DeptPortImpl.java`
- Create: `identity/infrastructure/TenantPortImpl.java`

- [ ] **Step 1: 创建 UserPortImpl.java**

从 `UserRepository` 改编，实现 `UserPort`，继承 `CommonRepository<SysUser, Long>`：

```java
package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTable;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.model.dto.UserSpecification;
import org.ninng.businesssvc.model.filter.CommandDataScopeFilter;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPortImpl extends CommonRepository<SysUser, Long> implements UserPort {

    private static final SysUserTable table = SysUserTable.$;

    public UserPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysUser> getTable() {
        return table;
    }

    @Nullable
    @Override
    @Cacheable(cacheNames = "u", key = "#username", unless = "#result==null")
    public UserDetailsView findByUsername(String username) {
        List<UserDetailsView> list = createQuery().where(table.name()
                        .eq(username))
                .select(table.fetch(UserDetailsView.class))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public Boolean update(UserUpdateInput input) {
        return sql.saveCommand(input)
                .setOptimisticLock(SysUserTable.class, new CommandDataScopeFilter<>())
                .execute()
                .isModified();
    }

    @Override
    public SysUser register(Input<SysUser> input) {
        return saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY)
                .execute()
                .getModifiedEntity();
    }

    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass) {
        return super.select(viewClass);
    }

    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification) {
        return createQuery().where(specification)
                .orderBy(getCreatedTable().createdAt().desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    @Override
    public Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
```

- [ ] **Step 2: 创建 UserRolePortImpl.java**

从 `UserRoleRepository` 改编，实现 `UserRolePort`：

```java
package org.ninng.businesssvc.identity.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.constant.C;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleTableEx;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTableEx;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.ninng.businesssvc.model.filter.TenantFilter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class UserRolePortImpl implements UserRolePort {

    private final JSqlClient sqlClient;

    public UserRolePortImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId) {
        return findByUserId(userId, false);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant) {
        AssociationTable<SysUser, SysUserTableEx, SysRole, SysRoleTableEx> associationTable = AssociationTable.of(
                SysUserTableEx.class, SysUserTableEx::roles);
        try {
            List<RoleDetailsView> list = sqlClient.filters(it -> {
                        if (disableTenant) {
                            it.disableByTypes(TenantFilter.class);
                        }
                    })
                    .createAssociationQuery(associationTable)
                    .where(Predicate.and(
                            associationTable.source().id().eq(userId),
                            associationTable.source().status().eq(C.Data.ENABLED),
                            associationTable.target().status().eq(C.Data.ENABLED)))
                    .select(associationTable.target().fetch(RoleDetailsView.class))
                    .execute();
            if (list == null) {
                return List.of();
            }
            return list;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return List.of();
        }
    }
}
```

- [ ] **Step 3: 创建 DeptPortImpl.java**

从 `DeptRepository` 改编，实现 `DeptPort`：

```java
package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptTable;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.ninng.businesssvc.model.dto.DeptSpecification;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DeptPortImpl extends CommonRepository<SysDept, Long> implements DeptPort {

    private static final SysDeptTable table = SysDeptTable.$;

    public DeptPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysDept> getTable() {
        return table;
    }

    @Override
    public SysDept create(Fetcher<SysDept> fetcher, Input<SysDept> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }

    @Override
    public Boolean deleteById(Long id) {
        return delete(id);
    }
}
```

- [ ] **Step 4: 创建 TenantPortImpl.java**

从 `TenantRepository` 改编，实现 `TenantPort`：

```java
package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantTable;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.model.dto.TenantSpecification;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TenantPortImpl extends CommonRepository<SysTenant, String> implements TenantPort {

    private static final SysTenantTable table = SysTenantTable.$;

    public TenantPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysTenant> getTable() {
        return table;
    }

    @Override
    public SysTenant create(Fetcher<SysTenant> fetcher, Input<SysTenant> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    public Boolean deleteById(String id) {
        return withUpdated().where(table.id().eq(id))
                .set(table.deletedAt(), LocalDateTime.now())
                .execute() > 0;
    }

    @Nullable
    @Override
    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code) {
        List<SysTenant> list = createQuery().where(table.code().eq(code))
                .select(table.fetch(fetcher))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/identity/infrastructure/
git commit -m "refactor: add UserPortImpl/UserRolePortImpl/DeptPortImpl/TenantPortImpl"
```

---

### Task 5: 创建应用服务

**Files:**
- Create: `identity/service/UserApplicationService.java`
- Create: `identity/service/DeptApplicationService.java`
- Create: `identity/service/TenantApplicationService.java`

- [ ] **Step 1: 创建 UserApplicationService.java**

从 `UserService` 改编，通过 `UserPort` 访问数据：

```java
package org.ninng.businesssvc.identity.service;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.identity.application.dto.RegisterInput;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.springframework.stereotype.Service;

@Service
public class UserApplicationService {

    private final UserPort userPort;

    public UserApplicationService(UserPort userPort) {
        this.userPort = userPort;
    }

    public SysUser register(RegisterInput registerInput) {
        return userPort.register((Input<SysUser>) registerInput);
    }

    public Boolean update(UserUpdateInput input) {
        return userPort.update(input);
    }
}
```

- [ ] **Step 2: 创建 DeptApplicationService.java**

从 `DeptService` 改编，通过 `DeptPort` 访问数据：

```java
package org.ninng.businesssvc.identity.service;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.ninng.businesssvc.model.dto.DeptCreateInput;
import org.ninng.businesssvc.model.dto.DeptSpecification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptApplicationService {

    private final DeptPort deptPort;

    public DeptApplicationService(DeptPort deptPort) {
        this.deptPort = deptPort;
    }

    public SysDept create(Fetcher<SysDept> fetcher, DeptCreateInput input) {
        return deptPort.create(fetcher, (Input<SysDept>) input);
    }

    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return deptPort.list(fetcher, pageReq, specification);
    }

    public Boolean deleteById(Long id) {
        // 数据权限校验由 PortImpl 的 delete 方法处理
        return deptPort.deleteById(id);
    }
}
```

- [ ] **Step 3: 创建 TenantApplicationService.java**

从 `TenantService` 改编，通过 `TenantPort` 访问数据：

```java
package org.ninng.businesssvc.identity.service;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.model.dto.TenantCreateInput;
import org.ninng.businesssvc.model.dto.TenantSpecification;
import org.ninng.businesssvc.utils.RandomStringIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class TenantApplicationService {

    private final TenantPort tenantPort;

    public TenantApplicationService(TenantPort tenantPort) {
        this.tenantPort = tenantPort;
    }

    public SysTenant create(Fetcher<SysTenant> fetcher, TenantCreateInput input) {
        return tenantPort.create(fetcher, draft -> {
            draft.setName(input.getName());
            draft.setCode(RandomStringIdGenerator.randomTenantCode());
        });
    }

    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String tenantCode) {
        return tenantPort.findByCode(fetcher, tenantCode);
    }

    public Boolean deleteById(String id) {
        return tenantPort.deleteById(id);
    }

    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq,
                                TenantSpecification specification) {
        return tenantPort.list(fetcher, pageReq, specification);
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/identity/service/
git commit -m "refactor: add UserApplicationService/DeptApplicationService/TenantApplicationService"
```

---

### Task 6: 移动控制器

**Files:**
- Move: `controller/UserController.java` → `identity/rest/UserController.java`
- Move: `controller/DeptController.java` → `identity/rest/DeptController.java`
- Move: `controller/TenantController.java` → `identity/rest/TenantController.java`

- [ ] **Step 1: 移动并更新 UserController.java**

将 UserController 写入 `identity/rest/UserController.java`：
- 更新 package 为 `org.ninng.businesssvc.identity.rest`
- 将 `UserService` 注入改为 `UserApplicationService`
- 将 `UserRepository` 直接调用改为通过 `UserApplicationService` 或 `UserPort`
- 更新所有 entity/dto import 为新路径

```java
package org.ninng.businesssvc.identity.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.UserSelectionView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRoleFetcher;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScopeFetcher;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserFetcher;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.service.UserApplicationService;
import org.ninng.businesssvc.version.ApiVersion;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Fetcher<SysUser> DEFAULT_FETCHER = SysUserFetcher.$.allScalarFields()
            .password(false)
            .roles(SysRoleFetcher.$.allScalarFields()
                    .roleIdScopes(SysRoleIdScopeFetcher.$.status()
                            .dataId()));

    private final UserApplicationService userApplicationService;
    private final UserPort userPort;

    public UserController(UserApplicationService userApplicationService, UserPort userPort) {
        this.userApplicationService = userApplicationService;
        this.userPort = userPort;
    }

    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysUser>> list(@RequestBody UserSpecification specification,
                                                             PageReq pageReq) {
        return R.ok(userPort.select(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("update")
    public R<Boolean> update(@RequestBody UserUpdateInput input) {
        return R.ok(userApplicationService.update(input));
    }

    @PostMapping("selections")
    public R<List<UserSelectionView>> selections() {
        return R.ok(userPort.select(UserSelectionView.class));
    }

    @ApiVersion(value = "1.6")
    @PostMapping("test")
    public R<Void> test(@RequestBody UserUpdateInput input) {
        System.out.println(input);
        return R.ok(null);
    }

    @PostMapping("test")
    @ApiVersion(deprecated = true)
    public R<Void> test1(@RequestBody UserUpdateInput input) {
        userPort.select(DEFAULT_FETCHER);
        System.out.println(input);
        return R.ok(null);
    }
}
```

- [ ] **Step 2: 移动并更新 DeptController.java**

```java
package org.ninng.businesssvc.identity.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.DeptCreateInput;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptFetcher;
import org.ninng.businesssvc.identity.service.DeptApplicationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dept")
public class DeptController {

    private static final Fetcher<SysDept> DEFAULT_FETCHER = SysDeptFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysDept> CREATE_FETCHER = SysDeptFetcher.$.name();

    private final DeptApplicationService deptApplicationService;

    public DeptController(DeptApplicationService deptApplicationService) {
        this.deptApplicationService = deptApplicationService;
    }

    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysDept> create(@RequestBody @Validated DeptCreateInput input) {
        return R.ok(deptApplicationService.create(CREATE_FETCHER, input));
    }

    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysDept>> list(
            @RequestBody @Validated DeptSpecification specification, @Validated PageReq pageReq) {
        return R.ok(deptApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptApplicationService.deleteById(id));
    }
}
```

- [ ] **Step 3: 移动并更新 TenantController.java**

```java
package org.ninng.businesssvc.identity.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.TenantCreateInput;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantFetcher;
import org.ninng.businesssvc.identity.service.TenantApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private static final Fetcher<SysTenant> DEFAULT_FETCHER = SysTenantFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysTenant> CREATE_FETCHER = SysTenantFetcher.$.name()
            .code();

    private final TenantApplicationService tenantApplicationService;

    public TenantController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysTenant> create(@RequestBody TenantCreateInput input) {
        return R.ok(tenantApplicationService.create(CREATE_FETCHER, input));
    }

    @PostMapping("list")
    public R<Page<@FetchBy("CREATE_FETCHER") SysTenant>> list(@RequestBody TenantSpecification specification,
                                                              PageReq pageReq) {
        return R.ok(tenantApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("/get/{tenantCode}")
    public R<@FetchBy("DEFAULT_FETCHER") SysTenant> get(@PathVariable String tenantCode) {
        return R.ok(tenantApplicationService.findByCode(DEFAULT_FETCHER, tenantCode));
    }

    @PostMapping("/delete:{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(tenantApplicationService.deleteById(id));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add business-svc/src/main/java/org/ninng/businesssvc/identity/rest/
git commit -m "refactor: move UserController/DeptController/TenantController to identity/rest"
```

---

### Task 7: 更新 identity 外部的所有引用

**Files:** 所有引用旧包路径的文件

- [ ] **Step 1: 查找所有引用旧包的文件**

```bash
cd business-svc/src/main/java
grep -r "org.ninng.businesssvc.model.SysUser" --include="*.java" -l
grep -r "org.ninng.businesssvc.model.SysDept" --include="*.java" -l
grep -r "org.ninng.businesssvc.model.SysDeptClosure" --include="*.java" -l
grep -r "org.ninng.businesssvc.model.SysTenant" --include="*.java" -l
grep -r "org.ninng.businesssvc.repository.UserRepository" --include="*.java" -l
grep -r "org.ninng.businesssvc.repository.UserRoleRepository" --include="*.java" -l
grep -r "org.ninng.businesssvc.repository.DeptRepository" --include="*.java" -l
grep -r "org.ninng.businesssvc.repository.TenantRepository" --include="*.java" -l
grep -r "org.ninng.businesssvc.service.UserService" --include="*.java" -l
grep -r "org.ninng.businesssvc.service.DeptService" --include="*.java" -l
grep -r "org.ninng.businesssvc.service.TenantService" --include="*.java" -l
grep -r "org.ninng.businesssvc.controller.UserController" --include="*.java" -l
grep -r "org.ninng.businesssvc.controller.DeptController" --include="*.java" -l
grep -r "org.ninng.businesssvc.controller.TenantController" --include="*.java" -l
```

- [ ] **Step 2: 更新每个文件的 import 语句**

对每个 grep 结果文件，进行对应替换（使用 replace_all）：

**已知需要更新的关键文件：**

1. **`component/DatabaseUserDetailsService.java`**
   - `org.ninng.businesssvc.model.SysUser` → `org.ninng.businesssvc.identity.domain.model.SysUser`
   - `org.ninng.businesssvc.model.dto.UserDetailsView` → `org.ninng.businesssvc.identity.application.dto.UserDetailsView`
   - `org.ninng.businesssvc.repository.UserRepository` → `org.ninng.businesssvc.identity.domain.port.UserPort` + 改为 `UserPort`
   - `org.ninng.businesssvc.repository.UserRoleRepository` → `org.ninng.businesssvc.identity.domain.port.UserRolePort`

2. **`context/UserContextHolder.java`**
   - `org.ninng.businesssvc.model.SysUser` → `org.ninng.businesssvc.identity.domain.model.SysUser`

3. **`filter/UserContextOnceFilterHandler.java`**
   - `org.ninng.businesssvc.model.SysUser` → `org.ninng.businesssvc.identity.domain.model.SysUser`

4. **`model/filter/TenantFilter.java`**
   - `org.ninng.businesssvc.model.SysTenant` → `org.ninng.businesssvc.identity.domain.model.SysTenant`

5. **`model/filter/TenantScope.java`**
   - `org.ninng.businesssvc.model.SysTenant` → `org.ninng.businesssvc.identity.domain.model.SysTenant`

6. **`model/filter/TenantDraftInterceptor.java`**
   - `org.ninng.businesssvc.model.SysTenant` → `org.ninng.businesssvc.identity.domain.model.SysTenant`

7. **`security/` 目录下引用 SysUser/SysDept 的文件**

8. **`interceptor/` 目录下的文件**

9. **其他通过 grep 发现的所有文件**

- [ ] **Step 3: 使用 sed/replace_all 批量更新 import 路径**

对于每个旧 import 路径，使用 Edit 工具的 `replace_all` 进行替换：

```bash
# 实体引用替换
# org.ninng.businesssvc.model.SysUser → org.ninng.businesssvc.identity.domain.model.SysUser
# org.ninng.businesssvc.model.SysDept → org.ninng.businesssvc.identity.domain.model.SysDept
# org.ninng.businesssvc.model.SysDeptClosure → org.ninng.businesssvc.identity.domain.model.SysDeptClosure
# org.ninng.businesssvc.model.SysTenant → org.ninng.businesssvc.identity.domain.model.SysTenant

# DTO 引用替换（注意 UserDetailsView 等 DTO 类已移到 identity.application.dto 包，但 DeptCreateInput 等还在 model.dto）
# 但 DeptCreateInput、TenantCreateInput、DeptSpecification、TenantSpecification 等被 Service/Controller 使用的 DTO
# 现在也生成到 identity.application.dto 了！

# 注意：UserSpecification、UserSelectionView、UserUpdateInput、UserDetailsView 都在 identity.application.dto
# 但 DeptCreateInput -> identity.application.dto.DeptCreateInput (原：model.dto.DeptCreateInput)
# 等等
```

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "refactor: update cross-references for moved identity types"
```

---

### Task 8: 删除旧文件

**Files:**
- Delete: `model/SysUser.java`
- Delete: `model/SysDept.java`
- Delete: `model/SysDeptClosure.java`
- Delete: `model/SysTenant.java`
- Delete: `repository/UserRepository.java`
- Delete: `repository/UserRoleRepository.java`
- Delete: `repository/DeptRepository.java`
- Delete: `repository/TenantRepository.java`
- Delete: `service/UserService.java`
- Delete: `service/DeptService.java`
- Delete: `service/TenantService.java`
- Delete: `controller/UserController.java`
- Delete: `controller/DeptController.java`
- Delete: `controller/TenantController.java`

- [ ] **Step 1: 确认新文件都存在且引用已更新**

先执行编译检查，确认没有编译错误（见 Task 9）。

- [ ] **Step 2: 逐个删除旧文件**

```bash
rm business-svc/src/main/java/org/ninng/businesssvc/model/SysUser.java
rm business-svc/src/main/java/org/ninng/businesssvc/model/SysDept.java
rm business-svc/src/main/java/org/ninng/businesssvc/model/SysDeptClosure.java
rm business-svc/src/main/java/org/ninng/businesssvc/model/SysTenant.java
rm business-svc/src/main/java/org/ninng/businesssvc/repository/UserRepository.java
rm business-svc/src/main/java/org/ninng/businesssvc/repository/UserRoleRepository.java
rm business-svc/src/main/java/org/ninng/businesssvc/repository/DeptRepository.java
rm business-svc/src/main/java/org/ninng/businesssvc/repository/TenantRepository.java
rm business-svc/src/main/java/org/ninng/businesssvc/service/UserService.java
rm business-svc/src/main/java/org/ninng/businesssvc/service/DeptService.java
rm business-svc/src/main/java/org/ninng/businesssvc/service/TenantService.java
rm business-svc/src/main/java/org/ninng/businesssvc/controller/UserController.java
rm business-svc/src/main/java/org/ninng/businesssvc/controller/DeptController.java
rm business-svc/src/main/java/org/ninng/businesssvc/controller/TenantController.java
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: remove old entity/repository/service/controller files after identity migration"
```

---

### Task 9: 编译验证

- [ ] **Step 1: 清理并编译**

```bash
cd business-svc
./mvnw clean compile -DskipTests
```

预期：编译成功，无错误。

- [ ] **Step 2: 如果编译失败，逐个修复**

常见问题：
- Jimmer 生成的 `*Fetcher`、`*Table`、`*Props` 类还在旧包 → 清理 target 目录后重新编译
- import 遗漏或错误 → 使用 grep 查找残余旧引用
- Spring Bean 重复定义 → 确保旧文件已删除，避免同名 `@Repository`/`@Service`/`@RestController`

```bash
# 修复后重新编译
./mvnw clean compile -DskipTests
```

- [ ] **Step 3: 运行测试**

```bash
./mvnw test
```

预期：全部测试通过。

- [ ] **Step 4: 提交**

```bash
git commit -m "fix: resolve compilation issues after identity migration"
```

---

### 计划自检

1. **Spec 覆盖：** 所有 spec 中的映射都对应了具体 task
2. **占位符检查：** 代码块中无 TBD/TODO
3. **类型一致性：** Port 方法签名与 PortImpl 实现一致，ApplicationService 方法名与 Controller 调用一致
4. **DTO 注意事项：** 移动 DTO 目标包后，旧的 import（如 `model.dto.UserDetailsView`）需要全部替换为 `identity.application.dto.UserDetailsView`。但需要注意——`model.dto` 包下可能还有其他非 User/Dept/Tenant 的 DTO，不能全局替换 `model.dto`，需要精确替换每个具体的类名导入。
