package org.ninng.businesssvc.controller;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.model.SysDept;
import org.ninng.businesssvc.model.SysDeptFetcher;
import org.ninng.businesssvc.model.dto.DeptCreateInput;
import org.ninng.businesssvc.model.dto.DeptSpecification;
import org.ninng.businesssvc.service.DeptService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class DeptController {

    private static final String PREFIX = "dept";

    private static final Fetcher<SysDept> DEFAULT_FETCHER = SysDeptFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysDept> CREATE_FETCHER = SysDeptFetcher.$.name();

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @PostMapping(PREFIX + ":create")
    public R<@FetchBy("CREATE_FETCHER") SysDept> createTenant(@RequestBody @Validated DeptCreateInput input) {
        return R.ok(deptService.create(CREATE_FETCHER, input));
    }

    @PostMapping(PREFIX + ":list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysDept>> list(
            @RequestBody @Validated DeptSpecification deptSpecification, @Validated PageReq pageReq) {
        return R.ok(deptService.list(DEFAULT_FETCHER, pageReq, deptSpecification));
    }

    @PostMapping(PREFIX + "/{id}:delete")
    public R<Boolean> delete(@PathVariable UUID id) {
        return R.ok(deptService.deleteById(id));
    }
}
