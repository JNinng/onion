package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 角色数据范围关联实体的 ID 类型枚举，用于标识 {@code SysRoleIdScope.dataId} 字段的值类型。
 *
 * <p>在 {@code SysRoleIdScope} 关联表中，{@code data_id} 字段可存储用户 ID 或部门 ID，
 * 由本枚举配合 {@code data_id} 字段共同确定数据范围的精确目标。
 * 该枚举通过 Jimmer 的 {@code @EnumType(Strategy.ORDINAL)} 以序数持久化到数据库。
 *
 * <p>使用场景：当 {@link DataScope} 为 {@code SPECIFIED} 时，{@code dataId} 的类型为
 * {@code USER}；当 {@code DataScope} 为 {@code SPECIFIED_DEPT} 时，类型为 {@code DEPT}。
 *
 * @see DataScope
 * @see org.ninng.businesssvc.identity.domain.model.SysRoleIdScope
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum IdType {

    /**
     * 用户 ID：{@code data_id} 字段存储的是 {@code SysUser.id}，表示数据范围指向特定用户
     */
    @EnumItem(ordinal = 1)
    USER,

    /**
     * 部门 ID：{@code data_id} 字段存储的是 {@code SysDept.id}，表示数据范围指向特定部门
     */
    @EnumItem(ordinal = 2)
    DEPT
}
