package org.ninng.businesssvc.model.common;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.IdView;
import org.babyfish.jimmer.sql.JoinColumn;
import org.babyfish.jimmer.sql.ManyToOne;
import org.babyfish.jimmer.sql.MappedSuperclass;
import org.ninng.businesssvc.identity.domain.model.SysUser;

@MappedSuperclass
public interface OwnerAware {

    /**
     * 归属人 ID
     */
    @IdView(value = "ownerUser")
    @Nullable
    Long ownerUserId();

    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    @Nullable
    SysUser ownerUser();

    /**
     * 归属部门 ID
     */
    @IdView(value = "ownerDept")
    @Nullable
    Long ownerDeptId();

    @ManyToOne
    @JoinColumn(name = "owner_dept_id")
    @Nullable
    SysUser ownerDept();
}
