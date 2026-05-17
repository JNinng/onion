package org.ninng.businesssvc.utils;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;

public class SnowflakeIdGenerator implements UserIdGenerator<Long> {

    public static void init(int machineId) {
        IdGeneratorOptions idGeneratorOptions = new IdGeneratorOptions((short) machineId);
        idGeneratorOptions.WorkerIdBitLength = 13;
        YitIdHelper.setIdGenerator(idGeneratorOptions);
    }

    @Override
    public Long generate(Class<?> entityType) {
        return YitIdHelper.nextId();
    }
}
