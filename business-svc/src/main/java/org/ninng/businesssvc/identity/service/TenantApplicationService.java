package org.ninng.businesssvc.identity.service;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.model.dto.TenantCreateInput;
import org.ninng.businesssvc.model.dto.TenantSpecification;
import org.ninng.businesssvc.utils.RandomStringIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class TenantApplicationService {

    private final TenantPort tenantPort;

    public TenantApplicationService(TenantPort tenantPort) {
        this.tenantPort = tenantPort;
    }

    public SysTenant create(Fetcher<SysTenant> fetcher, TenantCreateInput input) {
        return tenantPort.create(fetcher, draft -> {
            draft.setName(input.getName());
            draft.setCode(RandomStringIdGenerator.randomTenantCode());
        });
    }

    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String tenantCode) {
        return tenantPort.findByCode(fetcher, tenantCode);
    }

    public Boolean deleteById(String id) {
        return tenantPort.deleteById(id);
    }

    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq,
                                TenantSpecification specification) {
        return tenantPort.list(fetcher, pageReq, specification);
    }
}
