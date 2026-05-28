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

    public InfoResp info() {
        InfoResp info = new InfoResp();
        List<String> algorithmNames = new ArrayList<>(algorithmHandlerFactory.getAlgorithmNames());
        if (Algorithm.NONE.getValue()
                .equals(securityParamConfig.getDefaultAlgorithm())) {
            algorithmNames.addFirst(Algorithm.NONE.getValue());
        }
        info.setAlgorithms(algorithmNames);
        return info;
    }

    public SecretResp secretReq(SecretReq secretReq) throws Exception {
        String clientPublicKey = secretReq.getClientPublicKey();

        String aesSecretKey = AES.generateRandomBytesToBase64(32);
        String hmacKey = AES.generateRandomBytesToBase64(16);

        return new SecretResp(AES.encryptAndSign(securityParamConfig.getAesSecretKey(), aesSecretKey, hmacKey),
                AES.encryptAndSign(securityParamConfig.getAesHmacKey(), aesSecretKey, hmacKey),
                RSA.encryptByPublicKey(aesSecretKey, clientPublicKey), RSA.encryptByPublicKey(hmacKey,
                clientPublicKey));
    }

    public LoginResp login(LoginInput loginInput) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginInput.getName(),
                        loginInput.getPassword()));

        final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String jwt = jwtTokenUtil.generateToken(userDetails.getUsername());
        return new LoginResp(jwt);
    }

    public SysUser register(RegisterInput registerInput) {
        registerInput.setPassword(passwordEncoder.encode(registerInput.getPassword()));
        return userApplicationService.register(registerInput);
    }
}
