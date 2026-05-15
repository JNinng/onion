package org.ninng.businesssvc.utils.security;

import org.apache.commons.codec.binary.Hex;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.SpringContextHolder;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加密解密工具类
 * <p>
 * 提供 AES-CBC 模式的加密解密功能，支持 HMAC-SHA256 签名验证，
 * 用于保护敏感数据传输的安全性。
 * </p>
 *
 * <p>
 * 主要功能：
 * </p>
 * <ul>
 * <li>AES-CBC 加密/解密</li>
 * <li>HMAC-SHA256 签名生成与验证</li>
 * <li>时间戳防重放攻击</li>
 * <li>安全的字符串比较（防时序攻击）</li>
 * </ul>
 *
 * @author ninng
 */
public class AES {

    /**
     * Base64 编码器
     */
    public static final Base64.Encoder ENCODER = Base64.getEncoder();

    /**
     * Base64 解码器
     */
    public static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * AES 加密算法：CBC模式，PKCS5填充
     */
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * HMAC 签名算法
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 默认过期时间：5分钟（毫秒）
     */
    private static final long EXPIRE_TIME = 5 * 60 * 1000;

    /**
     * AES 解密
     * <p>
     * 解密格式：IV_BASE64:密文_BASE64
     * </p>
     *
     * @param encryptedContent 加密内容，格式为 IV_BASE64:密文_BASE64
     * @param aesBase64Key     Base64 编码的 AES 密钥
     * @return 解密后的明文字符串
     * @throws Exception 解密失败时抛出异常
     */
    public static String decrypt(String encryptedContent, String aesBase64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder()
                .decode(aesBase64Key);
        // 拆分 IV 和密文（客户端格式：IV_BASE64:密文_BASE64）
        String[] parts = encryptedContent.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(SpringContextHolder.getBean(I18nUtil.class)
                    .getMessage("security.formatErr"));
        }

        byte[] iv = DECODER.decode(parts[0]);
        byte[] encryptedBytes = DECODER.decode(parts[1]);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedBytes);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * AES 加密（随机生成IV）
     * <p>
     * 加密格式：IV_BASE64 + ":" + CIPHER_TEXT_BASE64
     * 每次加密会随机生成16字节的IV，确保相同明文加密后密文不同
     * </p>
     *
     * @param content      待加密的明文内容
     * @param aesBase64Key Base64 编码的 AES 密钥
     * @return 加密后的字符串，格式为 IV_BASE64:密文_BASE64
     * @throws Exception 加密失败时抛出异常
     */
    public static String encrypt(String content, String aesBase64Key) throws Exception {
        // 生成 16 字节 IV（CBC 模式必需）
        byte[] iv = generateRandomBytes(16);
        return encrypt(content, aesBase64Key, iv);
    }

    /**
     * AES 加密（指定IV）
     * <p>
     * 加密格式：IV_BASE64 + ":" + CIPHER_TEXT_BASE64
     * 使用指定的IV进行加密，适用于需要确定性加密的场景
     * </p>
     *
     * @param content      待加密的明文内容
     * @param aesBase64Key Base64 编码的 AES 密钥
     * @param iv           初始化向量（IV）
     * @return 加密后的字符串，格式为 IV_BASE64:密文_BASE64
     * @throws Exception 加密失败时抛出异常
     */
    public static String encrypt(String content, String aesBase64Key, byte[] iv) throws Exception {
        byte[] keyBytes = Base64.getDecoder()
                .decode(aesBase64Key);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        return ENCODER.encodeToString(iv) + ":" + ENCODER.encodeToString(encrypted);
    }

    /**
     * 加密并签名
     * <p>
     * 将内容加密后添加 HMAC 签名，用于验证数据完整性和来源可信度。
     * 内部格式：时间戳:随机数:明文内容
     * 输出格式：签名:时间戳:密文
     * </p>
     * 密文格式为 IV_BASE64:密文_BASE64
     * </p>
     *
     * @param content      待加密的明文内容
     * @param aesBase64Key Base64 编码的 AES 密钥
     * @param hmanKey      Base64 编码的 HMAC 签名密钥
     * @return 签名后的加密字符串，格式为 签名:时间戳:密文
     * </p>
     * 密文格式为 IV_BASE64:密文_BASE64
     * @throws Exception 加密或签名失败时抛出异常
     */
    public static String encryptAndSign(String content, String aesBase64Key, String hmanKey) throws Exception {
        long timestamp = System.currentTimeMillis();
        String nonce = Hex.encodeHexString(generateRandomBytes(16));
        String cipher = encrypt(timestamp + ":" + nonce + ":" + content, aesBase64Key);
        String signSource = timestamp + ":" + cipher;
        String sign = AES.hmacSign(signSource, hmanKey);
        return sign + ":" + timestamp + ":" + cipher;
    }

    /**
     * 生成指定长度的随机字节数组
     * <p>
     * 使用 SecureRandom 生成密码学安全的随机字节
     * </p>
     *
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * 生成指定长度的随机字节数组并转换为 Base64 字符串
     *
     * @param length 字节数组长度
     * @return Base64 编码的随机字符串
     */
    public static String generateRandomBytesToBase64(int length) {
        return ENCODER.encodeToString(generateRandomBytes(length));
    }

    /**
     * HMAC-SHA256 签名
     * <p>
     * 使用 HMAC-SHA256 算法对数据进行签名，用于验证数据完整性
     * </p>
     *
     * @param signSource      待签名的源字符串
     * @param base64SecretKey Base64 编码的签名密钥
     * @return Base64 编码的签名字符串
     * @throws Exception 签名失败时抛出异常
     */
    public static String hmacSign(String signSource, String base64SecretKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder()
                .decode(base64SecretKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] signBytes = mac.doFinal(signSource.getBytes(StandardCharsets.UTF_8));
        return ENCODER.encodeToString(signBytes);
    }

    /**
     * 安全字符串比较（防止时序攻击）
     * <p>
     * 通过恒定时间比较算法，防止攻击者通过响应时间差异推断正确字符
     * </p>
     *
     * @param a 第一个字符串
     * @param b 第二个字符串
     * @return 如果两个字符串相等返回 true，否则返回 false
     */
    private static boolean secureCompare(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 验证签名并解密（使用默认过期时间）
     * <p>
     * 验证 HMAC 签名并解密内容，默认过期时间为 5 分钟
     * </p>
     *
     * @param cipherContent 签名加密内容，格式为 签名:时间戳:密文
     * @param aesBase64Key  Base64 编码的 AES 密钥
     * @param hmacBase64Key Base64 编码的 HMAC 签名密钥
     * @return 解密后的明文内容
     * @throws Exception 验证或解密失败时抛出异常
     */
    public static String verifyAndDecrypt(String cipherContent, String aesBase64Key,
                                          String hmacBase64Key) throws Exception {
        return verifyAndDecrypt(cipherContent, aesBase64Key, hmacBase64Key, EXPIRE_TIME);
    }

    /**
     * 验证签名并解密（指定过期时间）
     * <p>
     * 验证流程：
     * <ol>
     * <li>解析签名、时间戳、密文</li>
     * <li>验证时间戳是否在有效期内</li>
     * <li>验证 HMAC 签名是否正确</li>
     * <li>解密密文并提取原始内容</li>
     * </ol>
     * </p>
     *
     * @param cipherContent 签名加密内容，格式为 签名:时间戳:密文
     *                      <p>
     *                      密文格式为 IV_BASE64:密文_BASE64
     * @param aesBase64Key  Base64 编码的 AES 密钥
     * @param hmacBase64Key Base64 编码的 HMAC 签名密钥
     * @param expireTime    过期时间（毫秒），小于等于 0 表示不检查过期
     * @return 解密后的明文内容
     * @throws Exception 验证或解密失败时抛出异常
     */
    public static String verifyAndDecrypt(String cipherContent, String aesBase64Key, String hmacBase64Key,
                                          long expireTime) throws Exception {
        String[] parts = cipherContent.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException(SpringContextHolder.getBean(I18nUtil.class)
                    .getMessage("security.formatErr"));
        }

        String receivedSignature = parts[0];
        String timestamp = parts[1];
        String cipher = parts[2];

        final long currentTime = System.currentTimeMillis();
        long dataTime = Long.parseLong(timestamp);
        if (dataTime < 0) {
            throw new IllegalArgumentException(SpringContextHolder.getBean(I18nUtil.class)
                    .getMessage("security.expireOrSignErr"));
        }
        if (expireTime > 0 && currentTime - dataTime > expireTime) {
            throw new SecurityException(SpringContextHolder.getBean(I18nUtil.class)
                    .getMessage("security.expireOrSignErr"));
        }

        String signSource = timestamp + ":" + cipher;
        String hmacSign = hmacSign(signSource, hmacBase64Key);
        if (!secureCompare(hmacSign, receivedSignature)) {
            throw new SecurityException(SpringContextHolder.getBean(I18nUtil.class)
                    .getMessage("security.expireOrSignErr"));
        }

        final String[] plainParts = decrypt(cipher, aesBase64Key).split(":", 3);
        return plainParts[2];
    }
}
