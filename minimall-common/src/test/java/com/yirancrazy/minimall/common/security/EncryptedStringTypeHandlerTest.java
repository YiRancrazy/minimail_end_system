package com.yirancrazy.minimall.common.security;

import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EncryptedStringTypeHandlerTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class EncryptedStringTypeHandlerTest {

    private static final String BASE64_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final EncryptedStringTypeHandler handler = new EncryptedStringTypeHandler();

    @BeforeEach
    void setUp() throws Exception {
        EncryptionAutoConfiguration config = new EncryptionAutoConfiguration();
        VaultKeyProvider provider = new LocalVaultKeyProvider(BASE64_KEY);
        config.encryptionInitializer(provider).afterPropertiesSet();
    }

    @Test
    void getNullableResult_validCipher_decryptsToPlaintext() throws Exception {
        String plain = "13800001111";
        String cipher = AesEncryptor.encrypt(plain, EncryptionContext.getKey());
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn(cipher);

        assertEquals(plain, handler.getNullableResult(rs, "phone"));
    }

    @Test
    void getNullableResult_nullValue_returnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn(null);

        assertNull(handler.getNullableResult(rs, "email"));
    }

    @Test
    void getNullableResult_emptyValue_returnsEmpty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("");

        assertEquals("", handler.getNullableResult(rs, "email"));
    }

    @Test
    void getNullableResult_invalidCipher_throwsBizException() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn("not-a-valid-ciphertext");

        BizException ex = assertThrows(BizException.class, () -> handler.getNullableResult(rs, "phone"));
        assertEquals(CommonCode.SYS_ERROR, ex.getCode());
        assertEquals("敏感字段解密失败", ex.getMessage());
        assertNotNull(ex.getCause(), "原始异常应作为 cause 保留");
    }

    @Test
    void getNullableResult_wrongKey_preservesOriginalCause() throws Exception {
        // 用 KEY_A 加密，再用 KEY_B 解密，cause 应为 IllegalStateException（AES 解密失败）
        byte[] keyA = EncryptionContext.getKey();
        String cipher = AesEncryptor.encrypt("secret", keyA);
        byte[] keyB = "99999999999999999999999999999999".getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
