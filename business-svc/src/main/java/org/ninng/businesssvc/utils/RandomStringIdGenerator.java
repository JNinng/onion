package org.ninng.businesssvc.utils;


import org.apache.commons.lang3.RandomStringUtils;
import org.babyfish.jimmer.sql.meta.UserIdGenerator;
import org.ninng.businesssvc.constant.C;

public class RandomStringIdGenerator implements UserIdGenerator<String> {

    public static String randomTenantCode() {
        return RandomStringUtils.insecure()
                .next(C.TENANT_ID_LENGTH, C.LOWER_CASE_ID_ALPHANUMERIC);
    }

    @Override
    public String generate(Class<?> entityType) {
        return randomTenantCode();
    }
}
