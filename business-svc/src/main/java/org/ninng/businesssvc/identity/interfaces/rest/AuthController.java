package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.*;
import org.ninng.businesssvc.identity.application.dto.LoginInput;
import org.ninng.businesssvc.identity.application.dto.RegisterInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserFetcher;
import org.ninng.businesssvc.security.Security;
import org.ninng.businesssvc.service.AuthService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final static Fetcher<SysUser> REGISTER_FETCHER = SysUserFetcher.$.name()
            .nickname();

    private final I18nUtil i18nUtil;
    private final AuthService authService;

    public AuthController(I18nUtil i18nUtil, AuthService authService) {
        this.i18nUtil = i18nUtil;
        this.authService = authService;
    }

    @PostMapping("/info")
    @Security(enabled = false)
    public R<InfoResp> info() {
        return R.ok(authService.info());
    }

    @PostMapping("/secret")
    public R<SecretResp> secret(@RequestBody @Validated SecretReq secretReq) throws Exception {
        return R.ok(authService.secretReq(secretReq));
    }

    @PostMapping("/login")
    public R<LoginResp> login(@RequestBody LoginInput loginInput) {
        try {
            return UserContextHolder.withOwnerDisabled(() -> R.ok(authService.login(loginInput)));
        } catch (BadCredentialsException e) {
            return R.fail(i18nUtil.getMessage("auth.login.usernameOrPasswordErr"));
        }
    }

    @PostMapping("/register")
    public R<@FetchBy("REGISTER_FETCHER") SysUser> register(@RequestBody RegisterInput registerInput) {
        return R.ok(authService.register(registerInput));
    }
}
