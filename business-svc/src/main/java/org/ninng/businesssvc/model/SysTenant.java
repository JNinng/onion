package org.ninng.businesssvc.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;
import org.ninng.businesssvc.utils.TenantIdGenerator;

@Entity
public interface SysTenant extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = TenantIdGenerator.class)
    String id();

    /**
     * 租户名
     */
    String name();

    /**
     * 租户唯一编码
     */
    String code();
}
