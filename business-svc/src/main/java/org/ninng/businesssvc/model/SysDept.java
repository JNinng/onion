package org.ninng.businesssvc.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

import java.util.UUID;

@Entity
public interface SysDept extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    @Nullable
    @IdView
    UUID parentId();

    @Nullable
    @ManyToOne
    SysDept parent();

    String name();

    @Nullable
    @IdView
    UUID adminUserId();

    @Nullable
    @ManyToOne
    SysUser adminUser();
}
