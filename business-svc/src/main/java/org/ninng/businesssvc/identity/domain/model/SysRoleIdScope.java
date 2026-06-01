package org.ninng.businesssvc.identity.domain.model;

import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.identity.domain.type.IdType;
import org.ninng.businesssvc.model.SnowflakeIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

/**
 * 角色ID范围关联实体
 * <p>
 * <p>存储角色与可访问数据ID的多对多关联关系。
 * 当角色的{@link org.ninng.businesssvc.identity.domain.type.DataScope}为「指定人」
 * 或「指定部门」时，此表记录该角色具体可访问的用户ID或部门ID列表。</p>
 * <p>
 * <p>{@code type} + {@code dataId} 组成业务联合唯一键（通过{@code @Key}标记），
 * 确保同一角色不会重复关联同一数据ID。</p>
 * <p>
 * <p>通过 {@code @OnDissociate(DELETE)} 级联删除：
 * 当角色被删除时，其所有ID范围关联记录自动删除。</p>
 * <p>
 * <p>混入特性：</p>
 * <ul>
 * <li>{@link CreatedAware} — 记录创建时间与创建人</li>
 * <li>{@link UpdatedAware} — 记录更新时间与更新人</li>
 * <li>{@link StatusAware} — 支持逻辑删除（软删除）</li>
 * <li>{@link TenantAware} — 多租户隔离</li>
 * </ul>
 */
@Entity
public interface SysRoleIdScope extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * 关联记录唯一标识
     * <p>
     * <p>使用{@link SnowflakeIdGenerator}雪花算法生成全局唯一ID</p>
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 数据ID类型
     * <p>
     * <p>由{@link IdType}枚举定义，区分{@code dataId}是用户ID还是部门ID</p>
     */
    IdType type();

    /**
     * 所属角色
     * <p>
     * <p>多对一关联到{@link SysRole}。使用{@code @Key}标记为业务唯一键的一部分。
     * {@code @OnDissociate(DELETE)} 表示角色删除时自动级联删除此关联。</p>
     */
    @ManyToOne
    @OnDissociate(DissociateAction.DELETE)
    @Key
    SysRole role();

    /**
     * 数据ID
     * <p>
     * <p>与{@code type}组合形成业务唯一键。根据{@code type}的不同，
     * 该值可能为用户ID或部门ID。使用{@code @Key}标记。</p>
     */
    @Key
    long dataId();
}
