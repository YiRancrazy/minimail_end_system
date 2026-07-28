package com.yirancrazy.minimall.common.result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void pageResult_wrapsRecords() {
        PageResult<String> p = new PageResult<>(1L, 10L, 10L, 1L, List.of("a"));
        assertEquals(1L, p.getTotal());
        assertEquals(1, p.getRecords().size());
    }
}