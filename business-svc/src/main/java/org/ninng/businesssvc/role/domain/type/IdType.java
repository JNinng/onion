package org.ninng.businesssvc.role.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 角色数据 ID 表，data_id 类型
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum IdType {
    /**
     * 角色 ID
     */
    @EnumItem(ordinal = 1)
    USER,
    /**
     * 部门 ID
     */
    @EnumItem(ordinal = 2)
    DEPT;
}
