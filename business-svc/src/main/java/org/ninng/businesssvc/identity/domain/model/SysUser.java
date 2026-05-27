package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.model.common.*;
import org.ninng.businesssvc.utils.SnowflakeIdGenerator;

import java.util.List;

@Entity
public interface SysUser extends CreatedAware, UpdatedAware, StatusAware, OwnerAware, TenantAware {

    /**
     * ID
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

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

    @Nullable
    String remark();

    @ManyToMany
    @JoinTable(
            name = "user_role_mapping",
            joinColumnName = "user_id",
            inverseJoinColumnName = "role_id"
    )
    List<SysRole> roles();
}
