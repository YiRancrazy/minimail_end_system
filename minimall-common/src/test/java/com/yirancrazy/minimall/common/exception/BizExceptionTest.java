package com.yirancrazy.minimall.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 BizException 的字段承载语义和运行时异常继承关系。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class BizExceptionTest {

    /** * 验证异常字段；以三参数构造；读取码别名消息；预期与入参一致. */
    @Test
    void testCarriesCodeAndMessage() {
        BizException e = new BizException("14001", "TOKEN_EXPIRED", "Token 已过期");
        assertEquals("14001", e.getCode());
        assertEquals("TOKEN_EXPIRED", e.getAlias());
        assertEquals("Token 已过期", e.getMessage());
    }

    /** * 验证异常继承；创建业务异常；检查运行时类型；预期属于运行时异常. */
    @Test
    void testIsRuntimeException() {
        assertTrue(new BizException("14001", "x", "y") instanceof RuntimeException);
    }
}
