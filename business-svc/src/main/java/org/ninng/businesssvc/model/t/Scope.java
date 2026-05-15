package org.ninng.businesssvc.model.t;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 数据范围规则枚举
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum Scope {
    /**
     * 个人
     */
    @EnumItem(ordinal = 0)
    PERSONAL,
    /**
     * 本部门
     */
    @EnumItem(ordinal = 1)
    DEPARTMENT,
    /**
     * 本部门及以下部门
     */
    @EnumItem(ordinal = 2)
    DEPARTMENT_AND_SUBDEPARTMENT,
    /**
     * 指定人
     */
    @EnumItem(ordinal = 3)
    SPECIFIED,
    /**
     * 全租户
     */
    @EnumItem(ordinal = 4)
    ALL_TENANT,
    /**
     * 自定义
     */
    @EnumItem(ordinal = 5)
    CUSTOM
}
