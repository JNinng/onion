package org.ninng.businesssvc.model.common;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.ninng.businesssvc.model.SysTenant;

@MappedSuperclass
public interface TenantAware {

    /**
     * 租户 ID
     */
    @IdView
    @Nullable
    String tenantId();

    @ManyToOne
    @Nullable
    SysTenant tenant();
}
