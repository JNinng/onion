package org.ninng.businesssvc.model;

import com.github.yitter.idgen.YitIdHelper;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;

public class SnowflakeIdGenerator implements UserIdGenerator<Long> {

    @Override
    public Long generate(Class<?> entityType) {
        return YitIdHelper.nextId();
    }
}
