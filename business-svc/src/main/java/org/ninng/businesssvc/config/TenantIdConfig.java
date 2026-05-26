package org.ninng.businesssvc.config;

import jakarta.annotation.PostConstruct;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.utils.SnowflakeIdGenerator;
import org.ninng.businesssvc.utils.TenantIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantIdConfig {

    private final I18nUtil i18nUtil;
    @Value("${security.tenant-id.machine-id:1}")
    private int machineId;
    @Value("${security.tenant-id.time-mask:0xDEADBEEFCAFEBABE}")
    private String timeMaskHex;

    public TenantIdConfig(I18nUtil i18nUtil) {
        this.i18nUtil = i18nUtil;
    }

    @PostConstruct
    public void init() {
        String hex = timeMaskHex;
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException(
                    i18nUtil.getMessage("exception.timeMaskEmpty"));
        }
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        long mask = Long.parseUnsignedLong(hex, 16);
        TenantIdGenerator.init(machineId, mask);
        SnowflakeIdGenerator.init(machineId);
    }
}
