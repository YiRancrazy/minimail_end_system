package com.yirancrazy.minimall.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: EncryptionAutoConfiguration 单元测试，验证加密上下文初始化和 AES-256-GCM 加解密往返。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
class EncryptionAutoConfigurationTest {

    private static final String BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeEach
    void setUp() throws Exception {
        EncryptionAutoConfiguration config = new EncryptionAutoConfiguration();
        VaultKeyProvider provider = new LocalVaultKeyProvider(BASE64_KEY);
        config.encryptionInitializer(provider).afterPropertiesSet();
    }

    @Test
    void initializer_initializes_32byte_key() {
        byte[] key = EncryptionContext.getKey();
        assertNotNull(key);
        assertEquals(32, key.length);
    }

    @Test
    void aes_roundtrip_preserves_plaintext() {
        String plain = "13800001111";
        String cipher = AesEncryptor.encrypt(plain, EncryptionContext.getKey());
        assertNotEquals(plain, cipher);
        assertEquals(plain, AesEncryptor.decrypt(cipher, EncryptionContext.getKey()));
    }
}
