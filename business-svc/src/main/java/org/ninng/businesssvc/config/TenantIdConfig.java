package org.ninng.businesssvc.config;

import jakarta.annotation.PostConstruct;
import org.ninng.businesssvc.utils.TenantIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantIdConfig {

    @Value("${security.tenant-id.machine-id:1}")
    private int machineId;

    @Value("${security.tenant-id.time-mask:0xDEADBEEFCAFEBABE}")
    private String timeMaskHex;

    @PostConstruct
    public void init() {
        String hex = timeMaskHex;
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException("security.tenant-id.time-mask must not be empty");
        }
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        long mask = Long.parseUnsignedLong(hex, 16);
        TenantIdGenerator.init(machineId, mask);
    }
}
