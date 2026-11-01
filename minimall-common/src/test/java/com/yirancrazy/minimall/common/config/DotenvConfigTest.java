package com.yirancrazy.minimall.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 DotenvConfig 对敏感配置键的日志脱敏规则及普通配置透传行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class DotenvConfigTest {

    /** * 验证敏感键脱敏；提供密码密钥令牌等键；调用脱敏方法；预期全部返回星号掩码. */
    @Test
    void testRedactedValueMasksSensitiveKeys() {
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_JWT_SECRET", "secret-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIO_SECRET_KEY", "secret-value"));
        assertEquals("***", DotenvConfig.redactedValue("ALIPAY_PRIVATE_KEY", "private-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIO_ACCESS_KEY", "access-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_INTERNAL_TOKEN", "token-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_SECURITY_AES_KEY", "aes-value"));
        assertEquals("***", DotenvConfig.redactedValue("MYSQL_PASSWORD", "password-value"));
    }

    /** * 验证普通值透传；键为数据库主机；调用脱敏方法；预期返回原值. */
    @Test
    void testRedactedValueKeepsNonSensitiveValue() {
        assertEquals("localhost", DotenvConfig.redactedValue("MYSQL_HOST", "localhost"));
    }
}
