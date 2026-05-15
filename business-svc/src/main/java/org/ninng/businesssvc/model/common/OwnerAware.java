package org.ninng.businesssvc.model.common;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public interface OwnerAware {

    /**
     * 归属人 ID
     */
    @Nullable
    UUID ownerUserId();

    /**
     * 归属部门 ID
     */
    @Nullable
    UUID ownerDeptId();
}
