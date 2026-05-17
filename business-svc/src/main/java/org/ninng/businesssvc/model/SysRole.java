package org.ninng.businesssvc.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;
import org.ninng.businesssvc.model.t.Scope;
import org.ninng.businesssvc.utils.SnowflakeIdGenerator;

@Entity
public interface SysRole extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 父角色 ID
     */
    @Nullable
    @IdView
    Long parentId();

    @Nullable
    @ManyToOne
    SysRole parent();

    /**
     * 角色名
     */
    String name();

    /**
     * 数据范围规则：0-个人 1-本部门 2-部门及子部门 3-指定人 4-全租户 5-自定义
     */
    Scope scope();

    /**
     * 权限编码
     */
    @Nullable
    String code();
}
