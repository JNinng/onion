package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 数据范围规则枚举
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum DataScope {
    /**
     * 仅本人
     */
    @EnumItem(ordinal = 1)
    PERSONAL,
    /**
     * 本部门
     */
    @EnumItem(ordinal = 2)
    DEPARTMENT,
    /**
     * 本部门及子部门
     */
    @EnumItem(ordinal = 3)
    DEPARTMENT_AND_SUBDEPARTMENT,
    /**
     * 指定人
     */
    @EnumItem(ordinal = 4)
    SPECIFIED,
    /**
     * 指定部门
     */
    @EnumItem(ordinal = 5)
    SPECIFIED_DEPT,
    /**
     * 全租户
     */
    @EnumItem(ordinal = 6)
    ALL_TENANT
}
