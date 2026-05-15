package org.ninng.businesssvc.model;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.UUIDIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

import java.util.List;
import java.util.UUID;

@Entity
public interface SysUser extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = UUIDIdGenerator.class)
    UUID id();

    /**
     * 用户名
     */
    @NotNull
    String name();

    /**
     * 昵称
     */
    String nickname();

    /**
     * 密码
     */
    String password();

    @ManyToMany
    @JoinTable(
            name = "user_role_mapping",
            joinColumnName = "user_id",
            inverseJoinColumnName = "role_id"
    )
    List<SysRole> roles();
}
