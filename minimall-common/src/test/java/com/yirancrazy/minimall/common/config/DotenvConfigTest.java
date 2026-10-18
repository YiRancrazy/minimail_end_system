package com.yirancrazy.minimall.common.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: DotenvConfigTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class DotenvConfigTest {


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


    @Test
    void redactedValue_keepsNonSensitiveValue() {
        assertEquals("localhost", DotenvConfig.redactedValue("MYSQL_HOST", "localhost"));
    }
}
