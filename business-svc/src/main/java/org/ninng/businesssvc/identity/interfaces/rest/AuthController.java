package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.*;
import org.ninng.businesssvc.entity.exception.ErrCode;
import org.ninng.businesssvc.identity.application.AuthApplicationService;
import org.ninng.businesssvc.identity.application.dto.LoginInput;
import org.ninng.businesssvc.identity.application.dto.RegisterInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserFetcher;
import org.ninng.businesssvc.security.Security;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 REST 控制器（API v1）。
 *
 * <p>负责处理用户认证相关的 HTTP 请求，包括登录、注册、获取服务器公钥及密钥协商。
 * 所有接口均映射至 {@code /auth} 路径下。
 *
 * @author JNinng
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 注册接口的 Jimmer 抓取器，仅加载 {@code name} 和 {@code nickname} 字段，
     * 避免暴露密码等敏感信息。
     */
    private final static Fetcher<SysUser> REGISTER_FETCHER = SysUserFetcher.$.name()
            .nickname();

    private final I18nUtil i18nUtil;
    private final AuthApplicationService authApplicationService;

    /**
     * 构造认证控制器。
     *
     * @param i18nUtil               国际化工具，用于返回错误提示的本地化信息
     * @param authApplicationService 认证应用服务，处理登录、注册等核心业务逻辑
     */
    public AuthController(I18nUtil i18nUtil, AuthApplicationService authApplicationService) {
        this.i18nUtil = i18nUtil;
        this.authApplicationService = authApplicationService;
    }

    /**
     * 获取服务器信息（公钥等）。
     *
     * <p>该接口无需加密解密，已标记为 {@link Security#enabled()} = {@code false}。
     *
     * @return 包含服务器公钥等信息的 {@link InfoResp}
     */
    @PostMapping("/info")
    @Security(enabled = false)
    public R<InfoResp> info() {
        return R.ok(authApplicationService.info());
    }

    /**
     * 密钥协商接口。
     *
     * <p>客户端提交自身公钥等信息，服务端返回加密算法及会话密钥参数，
     * 用于后续请求体的加解密。
     *
     * @param secretReq 客户端密钥协商请求参数
     * @return 包含服务端算法与会话密钥信息的 {@link SecretResp}
     * @throws Exception 算法处理过程中的异常
     */
    @PostMapping("/secret")
    public R<SecretResp> secret(@RequestBody @Validated SecretReq secretReq) throws Exception {
        return R.ok(authApplicationService.secretReq(secretReq));
    }

    /**
     * 用户登录。
     *
     * <p>验证用户名与密码，成功后返回 JWT Token。
     * 登录时临时禁用 Owner 租户过滤，以便跨租户匹配用户。
     *
     * @param loginInput 登录请求参数（用户名、密码等）
     * @return 包含 JWT Token 及用户信息的 {@link LoginResp}；<p>
     * 若凭证无效则返回错误码 {@link ErrCode#SECURITY_EXCEPTION}
     */
    @PostMapping("/login")
    public R<LoginResp> login(@RequestBody LoginInput loginInput) {
        try {
            // 登录时临时禁用 Owner 数据过滤，确保能查询到对应用户
            return UserContextHolder.withOwnerDisabled(() -> R.ok(authApplicationService.login(loginInput)));
        } catch (BadCredentialsException e) {
            // 凭证校验失败，返回国际化后的错误提示
            return R.err(i18nUtil.getMessage("auth.login.usernameOrPasswordErr"), ErrCode.SECURITY_EXCEPTION);
        }
    }

    /**
     * 用户注册。
     *
     * <p>创建新用户账号，返回的 {@link SysUser} 仅包含 {@code name} 和 {@code nickname} 字段。
     *
     * @param registerInput 注册请求参数
     * @return 注册成功后的用户信息（脱敏字段）
     */
    @PostMapping("/register")
    public R<@FetchBy("REGISTER_FETCHER") SysUser> register(@RequestBody RegisterInput registerInput) {
        return R.ok(authApplicationService.register(registerInput));
    }
}
