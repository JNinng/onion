package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.OneToMany;
import org.ninng.businesssvc.identity.domain.type.DataScope;
import org.ninng.businesssvc.identity.domain.type.RoleType;
import org.ninng.businesssvc.model.SnowflakeIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

import java.util.List;

@Entity
public interface SysRole extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 角色名
     */
    String name();

    /**
     * 权限编码
     */
    String code();

    /**
     * 角色类型：1-系统预置(不可删除，核心权限不可剥离) 2-租户自定义
     */
    RoleType roleType();

    /**
     * 数据范围规则：1-仅本人 2-本部门 3-本部门及子部门 4-指定人 5-指定部门 6-全租户
     */
    DataScope dataScope();

    /**
     * 组织可见范围：限定角色可分配的部门层级。NULL=全租户可分配
     */
    @Nullable
    Long scopeDeptId();

    /**
     * 备注
     */
    @Nullable
    String remark();

    @OneToMany(mappedBy = "role")
    List<SysRoleIdScope> roleIdScopes();
}
