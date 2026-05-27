package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 角色类型枚举
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum RoleType {
    /**
     * 系统预置（不可删除，核心权限不可剥离）
     */
    @EnumItem(ordinal = 1)
    SYSTEM,
    /**
     * 租户自定义
     */
    @EnumItem(ordinal = 2)
    TENANT
}
