package org.ninng.businesssvc.service;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.model.Immutables;
import org.ninng.businesssvc.model.SysTenant;
import org.ninng.businesssvc.model.SysTenantTable;
import org.ninng.businesssvc.model.dto.TenantCreateInput;
import org.ninng.businesssvc.repository.TenantRepository;
import org.ninng.businesssvc.utils.RandomStringIdGenerator;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public SysTenant create(Fetcher<SysTenant> fetcher, TenantCreateInput input) {
        return tenantRepository.create(fetcher, Immutables.createSysTenant(draft -> {
            draft.setName(input.getName());
            draft.setCode(RandomStringIdGenerator.randomTenantCode());
        }));
    }

    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String tenantName) {
        return tenantRepository.findByCode(fetcher, tenantName);
    }

    public Boolean deleteById(String id) {
        return tenantRepository.delete(id);
    }

    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq,
                                JSpecification<SysTenant, SysTenantTable> specification) {
        return tenantRepository.list(fetcher, specification, pageReq);
    }
}
