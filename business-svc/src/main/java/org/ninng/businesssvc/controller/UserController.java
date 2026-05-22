package org.ninng.businesssvc.controller;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.model.SysRoleFetcher;
import org.ninng.businesssvc.model.SysUser;
import org.ninng.businesssvc.model.SysUserFetcher;
import org.ninng.businesssvc.model.dto.UserSelectionView;
import org.ninng.businesssvc.model.dto.UserSpecification;
import org.ninng.businesssvc.model.dto.UserUpdateInput;
import org.ninng.businesssvc.repository.UserRepository;
import org.ninng.businesssvc.service.UserService;
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
            .roles(SysRoleFetcher.$.allScalarFields());

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysUser>> list(@RequestBody UserSpecification specification,
                                                             PageReq pageReq) {
        return R.ok(userRepository.select(DEFAULT_FETCHER, pageReq, specification));
    }

    @PostMapping("update")
    public R<Boolean> update(@RequestBody UserUpdateInput input) {
        return R.ok(userService.update(input));
    }

    @PostMapping("selections")
    public R<List<UserSelectionView>> selections() {
        return R.ok(userRepository.select(UserSelectionView.class));
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
        System.out.println(input);
        return R.ok(null);
    }
}
