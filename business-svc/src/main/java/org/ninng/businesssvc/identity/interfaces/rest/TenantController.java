package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.TenantCreateInput;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantFetcher;
import org.ninng.businesssvc.identity.application.TenantApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private static final Fetcher<SysTenant> DEFAULT_FETCHER = SysTenantFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysTenant> CREATE_FETCHER = SysTenantFetcher.$.name()
            .code();

    private final TenantApplicationService tenantApplicationService;

    public TenantController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysTenant> create(@RequestBody TenantCreateInput input) {
        return R.ok(tenantApplicationService.create(CREATE_FETCHER, input));
    }

    @PostMapping("list")
    public R<Page<@FetchBy("CREATE_FETCHER") SysTenant>> list(@RequestBody TenantSpecification specification,
                                                              PageReq pageReq) {
        return R.ok(tenantApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("/get/{tenantCode}")
    public R<@FetchBy("DEFAULT_FETCHER") SysTenant> get(@PathVariable String tenantCode) {
        return R.ok(tenantApplicationService.findByCode(DEFAULT_FETCHER, tenantCode));
    }

    @PostMapping("/delete:{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(tenantApplicationService.removeById(id));
    }
}
