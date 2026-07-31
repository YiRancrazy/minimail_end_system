package com.yirancrazy.minimall.common.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: BizException 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class BizExceptionTest {

    @Test
    void carriesCodeAndMessage() {
        BizException e = new BizException("14001", "TOKEN_EXPIRED", "Token 已过期");
        assertEquals("14001", e.getCode());
        assertEquals("TOKEN_EXPIRED", e.getAlias());
        assertEquals("Token 已过期", e.getMessage());
    }

    @Test
    void isRuntimeException() {
        assertTrue(new BizException("14001", "x", "y") instanceof RuntimeException);
    }
}