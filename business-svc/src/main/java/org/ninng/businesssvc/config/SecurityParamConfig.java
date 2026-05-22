package org.ninng.businesssvc.config;

import lombok.Data;
import org.ninng.businesssvc.security.Algorithm;
import org.ninng.businesssvc.utils.security.AES;
import org.ninng.businesssvc.utils.security.RSA;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityParamConfig {

    private static KeyPair keyPair;
    /**
     * 设置为非 NONE 强制加密
     * NONE (默认)
     * AES（AES/CBC/PKCS5Padding + HmacSHA256）
     */
    private String defaultAlgorithm = Algorithm.NONE.getValue();
    /**
     * base64 编码
     */
    private String aesSecretKey;
    /**
     * base64 编码
     */
    private String aesHmacKey;
    private List<String> responseWhitelist = List.of("msg", "code");
    /**
     * base64 编码
     */
    private String rsaPublicKey;
    /**
     * base64 编码
     */
    private String rsaPrivateKey;

    {
        aesSecretKey = AES.ENCODER.encodeToString(AES.generateRandomBytes(32));
        aesHmacKey = AES.ENCODER.encodeToString(AES.generateRandomBytes(16));

        try {
            keyPair = RSA.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        rsaPublicKey = RSA.getPublicKeyBase64(keyPair);
        rsaPrivateKey = RSA.getPrivateKeyBase64(keyPair);
    }
}
