package org.ninng.businesssvc.identity.application;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.TenantCreateInput;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.utils.IdUtils;
import org.springframework.stereotype.Service;

@Service
public class TenantApplicationService {

    private final TenantPort tenantPort;

    public TenantApplicationService(TenantPort tenantPort) {
        this.tenantPort = tenantPort;
    }

    public SysTenant create(Fetcher<SysTenant> fetcher, TenantCreateInput input) {
        input.setCode(IdUtils.generateTenantCode());
        return tenantPort.create(fetcher, input);
    }

    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String tenantCode) {
        return tenantPort.findByCode(fetcher, tenantCode);
    }

    public Boolean removeById(String id) {
        return tenantPort.removeById(id);
    }

    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq,
                                TenantSpecification specification) {
        return tenantPort.list(fetcher, pageReq, specification);
    }
}
