package com.yirancrazy.minimall.common.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AES-256-GCM 字符串加解密工具，每次加密生成随机 12 字节 IV，并将 IV 与密文一起 Base64 编码。
 * 该工具只负责密码学转换，密钥生命周期由 VaultKeyProvider 和 EncryptionContext 管理。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class AesEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 加解密工具类禁止实例化。 */
    private AesEncryptor() {}

    /**
     * 使用 AES/GCM/NoPadding 加密 UTF-8 明文，并返回包含随机 IV 的 Base64 字符串。
     *
     * @param plaintext 待加密明文；null 或空字符串原样返回
     * @param key 32 字节 AES-256 密钥
     * @return Base64(IV + 密文 + GCM 标签)
     * @throws IllegalStateException 密钥非法或底层加密失败时抛出
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
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /**
     * 解码 Base64 后从首部读取 IV，并在校验 GCM 认证标签后还原 UTF-8 明文。
     *
     * @param ciphertext 加密方法生成的 Base64 字符串；null 或空字符串原样返回
     * @param key 32 字节 AES-256 密钥
     * @return 解密后的明文
     * @throws IllegalArgumentException Base64 非法或密文长度不足时抛出
     * @throws IllegalStateException 密钥不匹配、认证标签校验失败或其他解密错误时抛出
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
        } catch (IllegalArgumentException e) {
            // 输入非法（Base64 解码失败或密文过短）直接透传，不做系统错误包装
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AES 解密失败", e);
        }
    }
}
