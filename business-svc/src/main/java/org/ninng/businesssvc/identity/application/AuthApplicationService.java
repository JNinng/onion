package org.ninng.businesssvc.identity.application;

import org.ninng.businesssvc.component.JwtTokenUtil;
import org.ninng.businesssvc.config.SecurityParamConfig;
import org.ninng.businesssvc.entity.InfoResp;
import org.ninng.businesssvc.entity.LoginResp;
import org.ninng.businesssvc.entity.SecretReq;
import org.ninng.businesssvc.entity.SecretResp;
import org.ninng.businesssvc.identity.application.dto.LoginInput;
import org.ninng.businesssvc.identity.application.dto.RegisterInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.security.Algorithm;
import org.ninng.businesssvc.security.AlgorithmHandlerFactory;
import org.ninng.businesssvc.utils.security.AES;
import org.ninng.businesssvc.utils.security.RSA;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证应用服务，负责处理登录、注册、密钥协商等认证相关用例。
 * <p>
 * <p>该服务协调以下组件完成认证流程：
 * <ul>
 * <li>{@link AuthenticationManager} — Spring Security 认证管理器，校验用户名密码</li>
 * <li>{@link JwtTokenUtil} — JWT 令牌生成工具</li>
 * <li>{@link AlgorithmHandlerFactory} — 加解密算法处理器工厂，管理客户端支持的算法列表</li>
 * <li>{@link PasswordEncoder} — 密码编码器，对注册密码进行哈希处理</li>
 * </ul>
 *
 * @see UserApplicationService
 * @see JwtTokenUtil
 */
@Service
public class AuthApplicationService {

    private final UserApplicationService userApplicationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final SecurityParamConfig securityParamConfig;
    private final AlgorithmHandlerFactory algorithmHandlerFactory;

    public AuthApplicationService(UserApplicationService userApplicationService, JwtTokenUtil jwtTokenUtil,
                                  AuthenticationManager authenticationManager,
                                  PasswordEncoder passwordEncoder, SecurityParamConfig securityParamConfig,
                                  AlgorithmHandlerFactory algorithmHandlerFactory) {
        this.userApplicationService = userApplicationService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.securityParamConfig = securityParamConfig;
        this.algorithmHandlerFactory = algorithmHandlerFactory;
    }

    /**
     * 获取系统信息，返回当前支持的加解密算法列表。
     * <p>
     * <p>如果默认算法为 {@link Algorithm#NONE}（明文模式），则将其置于列表首位，
     * 以便客户端优先选择。
     *
     * @return 包含服务端支持的算法名称列表的 {@link InfoResp}
     */
    public InfoResp info() {
        InfoResp info = new InfoResp();
        // 收集所有已注册的算法名称
        List<String> algorithmNames = new ArrayList<>(algorithmHandlerFactory.getAlgorithmNames());
        // 若默认算法为明文模式，将其置于列表首位供客户端优先选择
        if (Algorithm.NONE.getValue()
                .equals(securityParamConfig.getDefaultAlgorithm())) {
            algorithmNames.addFirst(Algorithm.NONE.getValue());
        }
        info.setAlgorithms(algorithmNames);
        return info;
    }

    /**
     * 密钥协商：服务端生成 AES 对称密钥和 HMAC 签名密钥，使用客户端公钥加密后返回。
     * <p>
     * <p>流程说明：
     * <ol>
     * <li>生成随机的 AES 密钥（256位）和 HMAC 密钥（128位）</li>
     * <li>使用服务端 AES 密钥分别加密签名后返回密文</li>
     * <li>使用客户端 RSA 公钥加密 AES 和 HMAC 密钥，仅客户端可解密</li>
     * </ol>
     *
     * @param secretReq 客户端密钥协商请求，包含客户端 RSA 公钥
     * @return {@link SecretResp} 包含加密后的 AES 密钥、HMAC 密钥及其公钥加密密文
     * @throws Exception 加密过程中可能抛出的异常
     */
    public SecretResp secretReq(SecretReq secretReq) throws Exception {
        String clientPublicKey = secretReq.getClientPublicKey();

        // 生成 32 字节 AES 密钥和 16 字节 HMAC 密钥
        String aesSecretKey = AES.generateRandomBytesToBase64(32);
        String hmacKey = AES.generateRandomBytesToBase64(16);

        // 使用服务端密钥签名后返回，同时用客户端公钥加密以确保只有客户端能解密
        return new SecretResp(AES.encryptAndSign(securityParamConfig.getAesSecretKey(), aesSecretKey, hmacKey),
                AES.encryptAndSign(securityParamConfig.getAesHmacKey(), aesSecretKey, hmacKey),
                RSA.encryptByPublicKey(aesSecretKey, clientPublicKey), RSA.encryptByPublicKey(hmacKey,
                clientPublicKey));
    }

    /**
     * 用户登录：校验用户名密码，生成并返回 JWT 令牌。
     * <p>
     * <p>流程说明：
     * <ol>
     * <li>通过 {@link AuthenticationManager} 进行 Spring Security 认证</li>
     * <li>认证通过后获取 {@link UserDetails}</li>
     * <li>基于用户名生成 JWT 令牌</li>
     * </ol>
     *
     * @param loginInput 登录请求，包含用户名和密码
     * @return {@link LoginResp} 包含 JWT 令牌的登录响应
     */
    public LoginResp login(LoginInput loginInput) {
        // 委托 Spring Security AuthenticationManager 完成认证
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginInput.getName(),
                        loginInput.getPassword()));

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // 基于认证通过的用户名生成 JWT
        final String jwt = jwtTokenUtil.generateToken(userDetails.getUsername());
        return new LoginResp(jwt);
    }

    /**
     * 用户注册：对密码进行哈希处理后委托给 {@link UserApplicationService} 完成注册。
     *
     * @param registerInput 注册请求，包含用户名、密码等信息
     * @return 注册成功后的 {@link SysUser} 实体
     */
    public SysUser register(RegisterInput registerInput) {
        // 先对明文密码进行哈希编码
        registerInput.setPassword(passwordEncoder.encode(registerInput.getPassword()));
        return userApplicationService.register(registerInput);
    }
}
