package org.ninng.businesssvc.controller;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.model.SysTenant;
import org.ninng.businesssvc.model.SysTenantFetcher;
import org.ninng.businesssvc.model.dto.TenantCreateInput;
import org.ninng.businesssvc.model.dto.TenantSpecification;
import org.ninng.businesssvc.service.TenantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
public class TenantController {

    private static final Fetcher<SysTenant> DEFAULT_FETCHER = SysTenantFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysTenant> CREATE_FETCHER = SysTenantFetcher.$.name()
            .code();

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysTenant> createTenant(@RequestBody TenantCreateInput input) {
        return R.ok(tenantService.create(CREATE_FETCHER, input));
    }

    @PostMapping("list")
    public R<Page<@FetchBy("CREATE_FETCHER") SysTenant>> list(@RequestBody TenantSpecification tenantSpecification,
                                                              PageReq pageReq) {
        return R.ok(tenantService.list(DEFAULT_FETCHER, pageReq, tenantSpecification));
    }

    @PostMapping("/get/{tenantCode}")
    public R<@FetchBy("DEFAULT_FETCHER") SysTenant> get(@PathVariable String tenantCode) {
        return R.ok(tenantService.findByCode(DEFAULT_FETCHER, tenantCode));
    }

    @PostMapping("/delelte:{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(tenantService.deleteById(id));
    }
}
