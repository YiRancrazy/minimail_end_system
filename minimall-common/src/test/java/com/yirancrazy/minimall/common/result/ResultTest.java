package com.yirancrazy.minimall.common.result;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Result 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class ResultTest {

    @Test
    void success_hasCodeZeroAndData() {
        Result<String> r = Result.success("hi");
        assertEquals("00000", r.getCode());
        assertNull(r.getMessage());
        assertEquals("hi", r.getData());
    }

    @Test
    void fail_keepsCodeAndMessage() {
        Result<Void> r = Result.fail("14001", "未授权");
        assertEquals("14001", r.getCode());
        assertEquals("未授权", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void success_fillsTraceIdFromMdc() {
        MDC.put(Result.TRACE_ID_KEY, "tid-123");
        try {
            Result<String> r = Result.success("hi");
            assertEquals("tid-123", r.getTraceId());
        }
        finally {
            MDC.clear();
        }
    }

    @Test
    void fail_keepsTraceIdFromMdc() {
        MDC.put(Result.TRACE_ID_KEY, "tid-456");
        try {
            Result<Void> r = Result.fail("14001", "未授权");
            assertEquals("tid-456", r.getTraceId());
        }
        finally {
            MDC.clear();
        }
    }

    @Test
    void pageResult_wrapsRecords() {
        PageResult<String> p = new PageResult<>(1L, 10L, 10L, 1L, List.of("a"));
        assertEquals(1L, p.getTotal());
        assertEquals(1, p.getRecords().size());
    }
}