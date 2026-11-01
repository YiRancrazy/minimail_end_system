package com.yirancrazy.minimall.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 MyBatis 加密 TypeHandler 的读取解密、空值透传及异常 cause 保留。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class EncryptedStringTypeHandlerTest {

    private static final String BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();

    /**
     * 测试目标：为每个用例初始化可用的 AES-256 加密上下文。
     * 测试场景与前置条件：每个测试执行前调用。关键步骤：用固定 Base64 密钥创建提供者并初始化上下文。
     * 预期结果：EncryptionContext 可提供 32 字节密钥。
     */
    @BeforeEach
    void setUp() throws Exception {
        EncryptionAutoConfiguration config = new EncryptionAutoConfiguration();
        VaultKeyProvider provider = new LocalVaultKeyProvider(BASE64_KEY);
        config.encryptionInitializer(provider).afterPropertiesSet();
    }

    /** * 验证有效密文解密；结果集返回手机号密文；按列名读取；预期返回原手机号. */
    @Test
    void testGetNullableResultValidCipherDecryptsToPlaintext() throws Exception {
        String plain = "13800001111";
        String cipher = AesEncryptor.encrypt(plain, EncryptionContext.getKey());
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn(cipher);

        assertEquals(plain, handler.getNullableResult(rs, "phone"));
    }

    /** * 验证null透传；结果集返回null；按列名读取；预期返回null. */
    @Test
    void testGetNullableResultNullValueReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn(null);

        assertNull(handler.getNullableResult(rs, "email"));
    }

    /** * 验证空串透传；结果集返回空串；按列名读取；预期返回空串. */
    @Test
    void testGetNullableResultEmptyValueReturnsEmpty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("");

        assertEquals("", handler.getNullableResult(rs, "email"));
    }

    /** * 验证非法密文失败；结果集返回非法值；按列名读取；预期业务异常并保留原因. */
    @Test
    void testGetNullableResultInvalidCipherThrowsBizException() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn("not-a-valid-ciphertext");

        BizException ex =
                assertThrows(BizException.class, () -> handler.getNullableResult(rs, "phone"));
        assertEquals(CommonCode.SYS_ERROR, ex.getCode());
        assertEquals("敏感字段解密失败", ex.getMessage());
        assertNotNull(ex.getCause(), "原始异常应作为 cause 保留");
    }

    /** * 验证错误密钥诊断；旧密文用新密钥读取；调用处理器；预期业务异常且原因为状态异常. */
    @Test
    void testGetNullableResultWrongKeyPreservesOriginalCause() throws Exception {
        // 用 KEY_A 加密，再用 KEY_B 解密，cause 应为 IllegalStateException（AES 解密失败）
        byte[] keyA = EncryptionContext.getKey();
        String cipher = AesEncryptor.encrypt("secret", keyA);
        byte[] keyB =
                "99999999999999999999999999999999".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        EncryptionContext.initialize(keyB);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn(cipher);

        BizException ex = assertThrows(BizException.class, () -> handler.getNullableResult(rs, "name"));
        assertEquals("敏感字段解密失败", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals("IllegalStateException", ex.getCause().getClass().getSimpleName());

        // 恢复原始密钥，避免污染其他测试
        EncryptionContext.initialize(keyA);
    }
}
