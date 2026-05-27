package org.ninng.businesssvc.identity.rest;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.RoleCreateInput;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleFetcher;
import org.ninng.businesssvc.identity.service.RoleApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/role")
public class RoleController {

    private static final Fetcher<SysRole> DEFAULT_FETCHER = SysRoleFetcher.$.allScalarFields();

    private final RoleApplicationService roleApplicationService;

    public RoleController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @RequestMapping("create")
    public R<@FetchBy("DEFAULT_FETCHER") SysRole> create(@RequestBody RoleCreateInput input) {
        return R.ok(roleApplicationService.create(DEFAULT_FETCHER, input));
    }

    @PostMapping("update")
    public R<Boolean> update(@RequestBody RoleUpdateInput input) {
        var roleIdScopes = input.getRoleIdScopes();
        if (roleIdScopes != null) {
            roleIdScopes.forEach(scope -> scope.setRoleId(input.getId()));
        }
        return R.ok(roleApplicationService.update(input));
    }
}
