package com.yirancrazy.minimall.common.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AES-256-GCM加密工具类，提供静态加密/解密方法。IV为12字节随机数，认证标签为16字节，输出格式为Base64(IV || Ciphertext || Tag)。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public final class AesEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesEncryptor() {
    }

    /**
     * 使用AES-256-GCM加密明文字符串。
     *
     * @param plaintext 明文字符串
     * @param key 32字节AES-256密钥
     * @return Base64编码的密文（IV || Ciphertext || Tag）
     */
    public static String encrypt(String plaintext, byte[] key) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        }
        catch (Exception e) {
            throw new IllegalStateException("AES encryption failed", e);
        }
    }

    /**
     * 使用AES-256-GCM解密密文字符串。
     *
     * @param ciphertext Base64编码的密文（IV || Ciphertext || Tag）
     * @param key 32字节AES-256密钥
     * @return 明文字符串
     */
    public static String decrypt(String ciphertext, byte[] key) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= IV_LENGTH) {
                // 密文至少应包含 IV 与认证标签，长度不足视为调用方输入非法而非系统错误
                throw new IllegalArgumentException("密文长度非法");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] ct = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ct, 0, ct.length);
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            byte[] plaintext = cipher.doFinal(ct);
            return new String(plaintext, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e) {
            // 输入非法（Base64 解码失败或密文过短）直接透传，不做系统错误包装
            throw e;
        }
        catch (Exception e) {
            throw new IllegalStateException("AES decryption failed", e);
        }
    }
}
