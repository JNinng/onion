package org.ninng.businesssvc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ninng.businesssvc.config.SecurityParamConfig;
import org.ninng.businesssvc.utils.security.AES;
import org.springframework.stereotype.Component;

@Component
public class AesHandler implements AlgorithmHandler {

    private final SecurityParamConfig securityParamConfig;
    private final ObjectMapper objectMapper;

    public AesHandler(SecurityParamConfig securityParamConfig, ObjectMapper objectMapper) {
        this.securityParamConfig = securityParamConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    public String encrypt(Object plain) throws Exception {
        switch (plain) {
            case null -> {
                return null;
            }
            case String plaintext -> {
                return AES.encryptAndSign(plaintext, securityParamConfig.getAesSecretKey(),
                        securityParamConfig.getAesHmacKey());
            }
            default -> {
                return AES.encryptAndSign(objectMapper.writeValueAsString(plain), securityParamConfig.getAesSecretKey(),
                        securityParamConfig.getAesHmacKey());
            }
        }
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        return AES.verifyAndDecrypt(cipherText, securityParamConfig.getAesSecretKey(),
                securityParamConfig.getAesHmacKey());
    }

    @Override
    public String support() {
        return Algorithm.AES.getValue();
    }
}
