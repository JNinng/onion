package org.ninng.businesssvc.utils.security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.security.Signature.getInstance;

public class RSA {

    // 加密算法
    private static final String ALGORITHM = "RSA";
    // 密钥长度，建议至少 2048 位
    private static final int KEY_SIZE = 4096;
    // 签名算法
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * 生成密钥对
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(KEY_SIZE);
    }

    /**
     * 生成密钥对
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * 获取公钥字符串
     */
    public static String getPublicKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder()
                .encodeToString(keyPair.getPublic()
                        .getEncoded());
    }

    /**
     * 获取私钥字符串
     */
    public static String getPrivateKeyBase64(KeyPair keyPair) {
        return Base64.getEncoder()
                .encodeToString(keyPair.getPrivate()
                        .getEncoded());
    }

    /**
     * 将字符串还原为公钥对象
     */
    public static PublicKey restorePublicKey(
            String publicKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder()
                .decode(publicKeyBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 将字符串还原为私钥对象
     */
    public static PrivateKey restorePrivateKey(
            String privateKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder()
                .decode(privateKeyBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 公钥加密
     *
     * @param data            待加密数据
     * @param publicKeyBase64 公钥字符串
     */
    public static String encryptByPublicKey(String data,
                                            String publicKeyBase64) throws NoSuchPaddingException,
            NoSuchAlgorithmException
            , InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PublicKey pubKey = restorePublicKey(publicKeyBase64);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder()
                .encodeToString(encryptedBytes);
    }

    /**
     * 私钥解密
     *
     * @param encryptedData    加密后的数据
     * @param privateKeyBase64 私钥字符串
     */
    public static String decryptByPrivateKey(String encryptedData,
                                             String privateKeyBase64) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException {
        PrivateKey priKey = restorePrivateKey(privateKeyBase64);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder()
                .decode(encryptedData));
        return new String(decryptedBytes);
    }

    /**
     * 私钥签名
     *
     * @param data             原始数据
     * @param privateKeyBase64 私钥字符串
     */
    public static String sign(String data,
                              String privateKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, SignatureException {
        PrivateKey priKey = restorePrivateKey(privateKeyBase64);
        Signature signature = getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(priKey);
        signature.update(data.getBytes());
        return Base64.getEncoder()
                .encodeToString(signature.sign());
    }

    /**
     * 公钥验签
     *
     * @param data            原始数据
     * @param publicKeyBase64 公钥字符串
     * @param signStr         签名字符串
     */
    public static boolean verify(String data, String publicKeyBase64,
                                 String signStr) throws NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidKeyException, SignatureException {
        PublicKey pubKey = restorePublicKey(publicKeyBase64);
        Signature signature = getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(data.getBytes());
        return signature.verify(Base64.getDecoder()
                .decode(signStr));
    }
}
