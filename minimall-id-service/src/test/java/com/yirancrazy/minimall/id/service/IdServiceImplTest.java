package com.yirancrazy.minimall.id.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yirancrazy.minimall.id.service.impl.IdServiceImpl;

/**
 * IdServiceImpl 单元测试，覆盖 nextId 的正常、唯一性与 bizTag 透传路径。
 */
public class IdServiceImplTest {

    /**
     * 验证 nextId 返回正数。
     */
    @Test
    public void nextId_returns_positive_value() {
        IdServiceImpl service = new IdServiceImpl();
        long id = service.nextId("ORDER");
        assertTrue(id > 0L);
    }

    /**
     * 验证连续调用 nextId 返回递增且不同的 ID。
     */
    @Test
    public void nextId_returns_increasing_values() {
        IdServiceImpl service = new IdServiceImpl();
        long a = service.nextId("ORDER");
        long b = service.nextId("ORDER");
        long c = service.nextId("ORDER");
        assertTrue(b > a);
        assertTrue(c > b);
    }

    /**
     * 验证不同 bizTag 也走同一 Snowflake，ID 仍递增。
     */
    @Test
    public void nextId_ignores_bizTag_and_keeps_increasing() {
        IdServiceImpl service = new IdServiceImpl();
        long a = service.nextId("ORDER");
        long b = service.nextId("USER");
        long c = service.nextId("PAY");
        assertTrue(b > a);
        assertTrue(c > b);
        assertNotEquals(a, b);
        assertNotEquals(b, c);
    }

    /**
     * 验证 null bizTag 不影响 ID 生成。
     */
    @Test
    public void nextId_handles_null_bizTag() {
        IdServiceImpl service = new IdServiceImpl();
        long id = service.nextId(null);
        assertTrue(id > 0L);
    }

    /**
     * 验证连续两次调用返回的 ID 不同且均为正数。
     */
    @Test
    public void nextId_consecutive_calls_distinct_and_positive() {
        IdServiceImpl service = new IdServiceImpl();
        long a = service.nextId("ORDER");
        long b = service.nextId("ORDER");
        assertTrue(a > 0L);
        assertTrue(b > 0L);
        assertNotEquals(a, b);
    }
}
