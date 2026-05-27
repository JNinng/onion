package org.ninng.businesssvc.identity.domain.model;

import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.identity.domain.type.IdType;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;
import org.ninng.businesssvc.utils.SnowflakeIdGenerator;

@Entity
public interface SysRoleIdScope extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 数据 ID 类型
     */
    IdType type();

    /**
     * 角色 ID
     */
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    @Key
    SysRole role();

    /**
     * 数据 ID
     */
    @Key
    long dataId();
}
