package org.ninng.businesssvc.controller;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.model.SysRole;
import org.ninng.businesssvc.model.SysRoleFetcher;
import org.ninng.businesssvc.model.dto.RoleCreateInput;
import org.ninng.businesssvc.service.RoleService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class RoleController {

    private static final String PREFIX = "role";

    private static final Fetcher<SysRole> DEFAULT_FETCHER = SysRoleFetcher.$.allScalarFields();

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @RequestMapping(PREFIX + ":create")
    public R<@FetchBy("DEFAULT_FETCHER") SysRole> create(@RequestBody RoleCreateInput input) {
        return R.ok(roleService.create(DEFAULT_FETCHER, input));
    }
}
