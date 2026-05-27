package org.ninng.businesssvc.identity.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.dto.UserSelectionView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRoleFetcher;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScopeFetcher;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserFetcher;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.service.UserApplicationService;
import org.ninng.businesssvc.version.ApiVersion;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private static final Fetcher<SysUser> DEFAULT_FETCHER = SysUserFetcher.$.allScalarFields()
            .password(false)
            .roles(SysRoleFetcher.$.allScalarFields()
                    .roleIdScopes(SysRoleIdScopeFetcher.$.status()
                            .dataId()));

    private final UserApplicationService userApplicationService;
    private final UserPort userPort;

    public UserController(UserApplicationService userApplicationService, UserPort userPort) {
        this.userApplicationService = userApplicationService;
        this.userPort = userPort;
    }

    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysUser>> list(@RequestBody UserSpecification specification,
                                                             PageReq pageReq) {
        return R.ok(userPort.select(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("update")
    public R<Boolean> update(@RequestBody UserUpdateInput input) {
        return R.ok(userApplicationService.update(input));
    }

    @PostMapping("selections")
    public R<List<UserSelectionView>> selections() {
        return R.ok(userPort.select(UserSelectionView.class));
    }

    @ApiVersion(value = "1.6")
    @PostMapping("test")
    public R<Void> test(@RequestBody UserUpdateInput input) {
        System.out.println(input);
        return R.ok(null);
    }

    @PostMapping("test")
    @ApiVersion(deprecated = true)
    public R<Void> test1(@RequestBody UserUpdateInput input) {
        userPort.select(DEFAULT_FETCHER);
        System.out.println(input);
        return R.ok(null);
    }
}
