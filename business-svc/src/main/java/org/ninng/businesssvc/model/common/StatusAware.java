package org.ninng.businesssvc.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface StatusAware {

    /**
     * 数据状态，0：禁用、1：启用
     */
    int status();
}
