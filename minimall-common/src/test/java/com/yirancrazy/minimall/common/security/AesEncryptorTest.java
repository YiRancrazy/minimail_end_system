package com.yirancrazy.minimall.common.security;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AesEncryptor 单元测试，验证AES-256-GCM加密/解密的正确性、边界条件和异常处理。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
class AesEncryptorTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void encrypt_then_decrypt_round_trip() {
        String plaintext = "13800138000";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        String decrypted = AesEncryptor.decrypt(encrypted, KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_null_returns_null() {
        assertNull(AesEncryptor.encrypt(null, KEY));
    }

    @Test
    void encrypt_empty_returns_empty() {
        assertEquals("", AesEncryptor.encrypt("", KEY));
    }

    @Test
    void decrypt_null_returns_null() {
        assertNull(AesEncryptor.decrypt(null, KEY));
    }

    @Test
    void decrypt_empty_returns_empty() {
        assertEquals("", AesEncryptor.decrypt("", KEY));
    }

    @Test
    void same_plaintext_produces_different_ciphertexts() {
        String plaintext = "test@example.com";
        String encrypted1 = AesEncryptor.encrypt(plaintext, KEY);
        String encrypted2 = AesEncryptor.encrypt(plaintext, KEY);
        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plaintext, AesEncryptor.decrypt(encrypted1, KEY));
        assertEquals(plaintext, AesEncryptor.decrypt(encrypted2, KEY));
    }

    @Test
    void decrypt_with_wrong_key_throws_exception() {
        String plaintext = "sensitive data";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        byte[] wrongKey = "99999999999999999999999999999999".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> AesEncryptor.decrypt(encrypted, wrongKey));
    }

    @Test
    void encrypt_unicode_text_round_trip() {
        String plaintext = "张三\n北京市朝阳区建国路1号";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        String decrypted = AesEncryptor.decrypt(encrypted, KEY);
        assertEquals(plaintext, decrypted);
    }
}
