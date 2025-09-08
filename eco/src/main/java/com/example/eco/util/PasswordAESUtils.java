package com.example.eco.util;

import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


/**
 * AES 对称加密工具类（CBC 模式）
 */
public class PasswordAESUtils {

    // 密钥（16字节）
    private static final String KEY = "OkIfdcyH0CyCyork";

    // 初始化向量（IV，16字节）
    private static final String IV = "ABCDEFGHIJKLM_iv";

    // 加密算法：AES/CBC/PKCS5Padding
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * AES 加密（CBC 模式）
     * @param content 待加密内容
     * @return Base64 编码的加密结果
     */
    public static String encrypt(String content) {
        try {
            // 1. 创建 Cipher 实例，指定算法
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // 2. 初始化密钥和 IV
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

            // 3. 初始化加密模式
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 4. 执行加密
            byte[] encryptedBytes = cipher.doFinal(content.getBytes("UTF-8"));

            // 5. 返回 Base64 编码结果
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * AES 解密（CBC 模式）
     * @param encryptedStr Base64 编码的加密字符串
     * @return 解密后的原文
     */
    public static String decrypt(String encryptedStr) {
        try {
            // 1. 创建 Cipher 实例
            Cipher cipher = Cipher.getInstance(ALGORITHM);

            // 2. 初始化密钥和 IV
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());

            // 3. 初始化解密模式
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // 4. Base64 解码
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedStr);

            // 5. 执行解密
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 6. 返回解密后的字符串
            return new String(decryptedBytes, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException |
                java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        String originalText = "123456";
        System.out.println("原文: " + originalText);

        // 加密
        String encryptedText = encrypt(originalText);
        System.out.println("加密后: " + encryptedText);

        // 解密
        String decryptedText = decrypt(encryptedText);
        System.out.println("解密后: " + decryptedText);

        // 计算 SHA-1（如果需要）
        System.out.println("SHA-1: " + DigestUtils.sha1Hex(decryptedText));
    }
}