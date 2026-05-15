package org.ninng.businesssvc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecretResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -5597382852091880979L;

    /**
     * 接口通信对称密钥，base64 编码
     * <p>
     * 使用临时对称加密
     * <p>
     * secret：tempSymmetrySecretKey
     * hmac：tempSymmetryHmacKey
     */
    private String aesBase64Key;
    /**
     * 接口通信对称算法 hmac，base64 编码
     * <p>
     * 使用临时对称加密
     */
    private String hmacBase64Key;
    /**
     * 临时对称密钥
     * <p>
     * 由客户端公钥加密
     */
    private String tempAesKey;
    /**
     * 临时对称密 hmac
     * <p>
     * 由客户端公钥加密
     */
    private String tempHmacKey;
}
