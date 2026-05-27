package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;
import org.ninng.businesssvc.utils.SnowflakeIdGenerator;

@Entity
public interface SysDept extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    @Nullable
    @IdView
    Long parentId();

    @Nullable
    @ManyToOne
    SysDept parent();

    String name();

    @Nullable
    @IdView
    Long ownerUserId();

    @Nullable
    @ManyToOne
    SysUser ownerUser();
}
