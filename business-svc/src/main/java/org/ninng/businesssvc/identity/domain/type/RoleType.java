package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 角色类型枚举，标识角色的来源与可管理性。
 *
 * <p>该枚举通过 Jimmer 的 {@code @EnumType(Strategy.ORDINAL)} 以序数持久化到数据库。
 * 两种类型的核心区别在于：<b>系统角色</b>由平台预置，不可删除且核心权限不可剥离；
 * <b>租户角色</b>由各租户自行创建和管理，具备完全的增删改权限。
 *
 * <p>典型用法：在删除角色或修改角色权限时，首先检查 {@link #SYSTEM} 类型，
 * 若为系统角色则拒绝删除或拒绝剥离核心权限，仅允许租户自定义角色自由操作。
 *
 * @see DataScope
 * @see org.ninng.businesssvc.identity.domain.model.SysRole
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum RoleType {

    /**
     * 系统预置角色：由平台统一提供，<b>不可删除</b>，核心权限<b>不可剥离</b>。
     * <p>通常包括超级管理员、租户管理员等基础角色，确保平台基本运行能力。
     */
    @EnumItem(ordinal = 1)
    SYSTEM,

    /**
     * 租户自定义角色：由各租户管理员自行创建和维护，可自由增删改。
     * <p>权限范围和角色名称完全由租户自行定义，满足各租户的个性化权限管理需求。
     */
    @EnumItem(ordinal = 2)
    TENANT
}
