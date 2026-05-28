package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.DeptCreateInput;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptFetcher;
import org.ninng.businesssvc.identity.application.DeptApplicationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dept")
public class DeptController {

    private static final Fetcher<SysDept> DEFAULT_FETCHER = SysDeptFetcher.$.allScalarFields()
            .deletedAt(false);
    private static final Fetcher<SysDept> CREATE_FETCHER = SysDeptFetcher.$.name();

    private final DeptApplicationService deptApplicationService;

    public DeptController(DeptApplicationService deptApplicationService) {
        this.deptApplicationService = deptApplicationService;
    }

    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysDept> create(@RequestBody @Validated DeptCreateInput input) {
        return R.ok(deptApplicationService.create(CREATE_FETCHER, input));
    }

    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysDept>> list(
            @RequestBody @Validated DeptSpecification specification, @Validated PageReq pageReq) {
        return R.ok(deptApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptApplicationService.removeById(id));
    }
}
