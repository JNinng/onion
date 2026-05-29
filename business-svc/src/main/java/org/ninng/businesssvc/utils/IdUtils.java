package org.ninng.businesssvc.utils;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.ninng.businesssvc.constant.C;

public class IdUtils {

    public static void init(int machineId) {
        IdGeneratorOptions idGeneratorOptions = new IdGeneratorOptions((short) machineId);
        idGeneratorOptions.WorkerIdBitLength = 13;
        YitIdHelper.setIdGenerator(idGeneratorOptions);
    }

    public static String generateTenantCode() {
        return RandomStringUtils.insecure()
                .next(C.TENANT_ID_LENGTH, C.TENANT_ID_ALPHABET);
    }

    public static String generateTraceId() {
        return RandomStringUtils.insecure()
                .next(C.TRACE_ID_LENGTH, C.LOWER_CASE_ID_ALPHANUMERIC);
    }
}
