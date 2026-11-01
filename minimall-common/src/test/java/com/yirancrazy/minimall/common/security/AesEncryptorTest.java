package com.yirancrazy.minimall.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 AES-GCM 加解密的往返、随机 IV、Unicode 支持和异常分类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class AesEncryptorTest {

    private static final byte[] KEY =
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    /** * 验证加解密往返；使用32字节密钥；加密手机号后解密；预期还原原文. */
    @Test
    void testEncryptThenDecryptRoundTrip() {
        String plaintext = "13800138000";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        String decrypted = AesEncryptor.decrypt(encrypted, KEY);
        assertEquals(plaintext, decrypted);
    }

    /** * 验证null明文字符处理；输入null；调用加密；预期返回null. */
    @Test
    void testEncryptNullReturnsNull() {
        assertNull(AesEncryptor.encrypt(null, KEY));
    }

    /** * 验证空明文字符处理；输入空串；调用加密；预期返回空串. */
    @Test
    void testEncryptEmptyReturnsEmpty() {
        assertEquals("", AesEncryptor.encrypt("", KEY));
    }

    /** * 验证null密文字符处理；输入null；调用解密；预期返回null. */
    @Test
    void testDecryptNullReturnsNull() {
        assertNull(AesEncryptor.decrypt(null, KEY));
    }

    /** * 验证空密文字符处理；输入空串；调用解密；预期返回空串. */
    @Test
    void testDecryptEmptyReturnsEmpty() {
        assertEquals("", AesEncryptor.decrypt("", KEY));
    }

    /** * 验证随机IV；同明文连续加密；比较并分别解密；预期密文不同且均可还原. */
    @Test
    void testSamePlaintextProducesDifferentCiphertexts() {
        String plaintext = "test@example.com";
        String encrypted1 = AesEncryptor.encrypt(plaintext, KEY);
        String encrypted2 = AesEncryptor.encrypt(plaintext, KEY);
        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plaintext, AesEncryptor.decrypt(encrypted1, KEY));
        assertEquals(plaintext, AesEncryptor.decrypt(encrypted2, KEY));
    }

    /** * 验证错误密钥；用正确密钥加密后用另一密钥解密；调用解密；预期抛状态异常. */
    @Test
    void testDecryptWithWrongKeyThrowsException() {
        String plaintext = "sensitive data";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        byte[] wrongKey = "99999999999999999999999999999999".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> AesEncryptor.decrypt(encrypted, wrongKey));
    }

    /** * 验证Unicode往返；明文含姓名地址换行；加密后解密；预期文本和换行不变. */
    @Test
    void testEncryptUnicodeTextRoundTrip() {
        String plaintext = "张三\n北京市朝阳区建国路1号";
        String encrypted = AesEncryptor.encrypt(plaintext, KEY);
        String decrypted = AesEncryptor.decrypt(encrypted, KEY);
        assertEquals(plaintext, decrypted);
    }

    /** * 验证短密文校验；提供不足长度密文；调用解密；预期抛参数异常而非越界. */
    @Test
    void testDecryptShortCiphertextThrowsIllegalArgumentException() {
        // 密文长度不足 IV(12) + 认证标签(16) 时应抛明确参数异常而非 NegativeArraySizeException
        assertThrows(IllegalArgumentException.class, () -> AesEncryptor.decrypt("c2hvcnQ=", KEY));
    }
}
