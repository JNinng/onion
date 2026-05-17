package org.ninng.businesssvc.model.common;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface OwnerAware {

    /**
     * 归属人 ID
     */
    @Nullable
    Long ownerUserId();

    /**
     * 归属部门 ID
     */
    @Nullable
    Long ownerDeptId();
}
