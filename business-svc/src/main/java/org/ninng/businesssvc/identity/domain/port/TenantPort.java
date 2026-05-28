package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;

public interface TenantPort {

    SysTenant create(Fetcher<SysTenant> fetcher, org.babyfish.jimmer.Input<SysTenant> input);

    @Nullable
    SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code);

    Boolean removeById(String id);

    Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification);
}
