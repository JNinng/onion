package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.model.SnowflakeIdGenerator;
import org.ninng.businesssvc.model.common.*;

import java.util.List;

/**
 * 系统用户实体
 * <p>
 * <p>身份管理中的用户核心实体，承载用户认证信息和角色关联。
 * 通过多对多关联（中间表 {@code user_role_mapping}）与{@link SysRole}建立关系，
 * 实现RBAC权限模型中的用户-角色绑定。</p>
 * <p>
 * <p>密码字段在查询时默认不加载（通过 Fetcher 配置 {@code .password(false)}），
 * 确保密码不会意外泄漏到API响应中。</p>
 * <p>
 * <p>混入特性：</p>
 * <ul>
 * <li>{@link CreatedAware} — 记录创建时间与创建人</li>
 * <li>{@link UpdatedAware} — 记录更新时间与更新人</li>
 * <li>{@link StatusAware} — 支持逻辑删除（软删除）</li>
 * <li>{@link OwnerAware} — 记录数据所属人，用于数据所有权验证</li>
 * <li>{@link TenantAware} — 多租户隔离，每个用户归属特定租户</li>
 * </ul>
 */
@Entity
public interface SysUser extends CreatedAware, UpdatedAware, StatusAware, OwnerAware, TenantAware {

    /**
     * 用户唯一标识
     * <p>
     * <p>使用{@link SnowflakeIdGenerator}雪花算法生成全局唯一ID</p>
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 用户名（登录账号）
     * <p>
     * <p>不可为空（{@code @NotNull}），用于登录认证的唯一标识</p>
     */
    @NotNull
    String name();

    /**
     * 昵称（展示用）
     */
    String nickname();

    /**
     * 加密后的密码
     * <p>
     * <p>存储经{@code DelegatingPasswordEncoder}（BCrypt/SCrypt）加密后的密文。
     * 查询时默认不加载该字段以保障安全性。</p>
     */
    String password();

    /**
     * 备注
     */
    @Nullable
    String remark();

    /**
     * 用户拥有的角色列表
     * <p>
     * <p>多对多关联，通过中间表 {@code user_role_mapping} 与{@link SysRole}建立关系。
     * Jimmer {@code @JoinTable} 指定：
     * <ul>
     * <li>{@code name = "user_role_mapping"} — 中间表名称</li>
     * <li>{@code joinColumnName = "user_id"} — 本实体（用户）在中间表中的外键列</li>
     * <li>{@code inverseJoinColumnName = "role_id"} — 关联实体（角色）在中间表中的外键列</li>
     * </ul>
     * </p>
     */
    @ManyToMany
    @JoinTable(
            name = "user_role_mapping",
            joinColumnName = "user_id",
            inverseJoinColumnName = "role_id"
    )
    List<SysRole> roles();
}
