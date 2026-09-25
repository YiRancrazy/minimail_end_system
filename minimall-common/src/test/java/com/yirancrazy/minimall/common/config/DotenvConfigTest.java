package com.yirancrazy.minimall.common.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: DotenvConfig 单元测试，验证敏感环境变量日志脱敏规则。
 * @Version: 1.0
 * @DateTime: 2026/09/25
 **/
class DotenvConfigTest {

    /**
     * 验证密钥、Token 和私钥配置不会进入日志值。
     */
    @Test
    void redactedValue_masksSensitiveKeys() {
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_JWT_SECRET", "secret-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIO_SECRET_KEY", "secret-value"));
        assertEquals("***", DotenvConfig.redactedValue("ALIPAY_PRIVATE_KEY", "private-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIO_ACCESS_KEY", "access-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_INTERNAL_TOKEN", "token-value"));
        assertEquals("***", DotenvConfig.redactedValue("MINIMALL_SECURITY_AES_KEY", "aes-value"));
        assertEquals("***", DotenvConfig.redactedValue("MYSQL_PASSWORD", "password-value"));
    }

    /**
     * 验证非敏感配置仍按原值返回。
     */
    @Test
    void redactedValue_keepsNonSensitiveValue() {
        assertEquals("localhost", DotenvConfig.redactedValue("MYSQL_HOST", "localhost"));
    }
}
