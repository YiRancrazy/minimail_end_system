package com.yirancrazy.minimall.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证加密自动配置初始化密钥以及 AES 往返行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class EncryptionAutoConfigurationTest {

    private static final String BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /**
     * 测试目标：每次用例前建立有效加密上下文。
     * 测试场景与前置条件：使用固定 Base64 编码的 32 字节密钥。
     * 关键步骤：创建自动配置和本地密钥提供者，执行初始化器。预期结果：EncryptionContext 可读取密钥。
     */
    @BeforeEach
    void setUp() throws Exception {
        EncryptionAutoConfiguration config = new EncryptionAutoConfiguration();
        VaultKeyProvider provider = new LocalVaultKeyProvider(BASE64_KEY);
        config.encryptionInitializer(provider).afterPropertiesSet();
    }

    /** * 验证密钥初始化；启动自动配置；读取加密上下文；预期密钥非空且32字节. */
    @Test
    void testInitializerInitializes32byteKey() {
        byte[] key = EncryptionContext.getKey();
        assertNotNull(key);
        assertEquals(32, key.length);
    }

    /** * 验证上下文加解密；初始化固定密钥；加密手机号并解密；预期密文不同且原文还原. */
    @Test
    void testAesRoundtripPreservesPlaintext() {
        String plain = "13800001111";
        String cipher = AesEncryptor.encrypt(plain, EncryptionContext.getKey());
        assertNotEquals(plain, cipher);
        assertEquals(plain, AesEncryptor.decrypt(cipher, EncryptionContext.getKey()));
    }
}
