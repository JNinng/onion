package org.ninng.businesssvc.identity.domain.model;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import org.ninng.businesssvc.model.SnowflakeIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

/**
 * 系统部门实体
 * <p>
 * <p>表示组织架构中的部门节点，支持树形层级结构（自关联父子关系）。
 * 同时关联部门负责人（{@link SysUser}），与{@link SysDeptClosure}闭包表配合
 * 实现高效的子树查询。</p>
 * <p>
 * <p>混入特性：</p>
 * <ul>
 * <li>{@link CreatedAware} — 记录创建时间与创建人</li>
 * <li>{@link UpdatedAware} — 记录更新时间与更新人</li>
 * <li>{@link StatusAware} — 支持逻辑删除（软删除）</li>
 * <li>{@link TenantAware} — 多租户隔离，每个部门归属特定租户</li>
 * </ul>
 */
@Entity
public interface SysDept extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * 部门唯一标识
     * <p>
     * <p>使用{@link SnowflakeIdGenerator}雪花算法生成全局唯一ID</p>
     */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator.class)
    long id();

    /**
     * 父部门ID（外键视图）
     * <p>
     * <p>通过 Jimmer {@code @IdView} 直接获取父部门的外键ID，
     * 无需加载完整的父部门对象，减少不必要的数据库查询。</p>
     */
    @Nullable
    @IdView
    Long parentId();

    /**
     * 父部门对象引用
     * <p>
     * <p>自关联的{@link ManyToOne}多对一关系，指向上级部门。
     * 值为{@code null}时表示该部门为根节点。</p>
     */
    @Nullable
    @ManyToOne
    SysDept parent();

    /**
     * 部门名称
     */
    String name();

    /**
     * 部门负责人用户ID（外键视图）
     * <p>
     * <p>通过{@code @IdView}直接获取负责人用户的外键ID</p>
     */
    @Nullable
    @IdView
    Long ownerUserId();

    /**
     * 部门负责人对象引用
     * <p>
     * <p>关联到{@link SysUser}的多对一关系。
     * 值为{@code null}时表示未指定部门负责人。</p>
     */
    @Nullable
    @ManyToOne
    SysUser ownerUser();
}
