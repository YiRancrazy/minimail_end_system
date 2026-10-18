package com.yirancrazy.minimall.id.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yirancrazy.minimall.id.service.impl.IdServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class IdServiceImplTest {


    /**
     * 执行 nextId_returns_positive_value 方法。
     */
    public void nextId_returns_positive_value() {
        IdServiceImpl service = new IdServiceImpl("1", "1");
        long id = service.nextId("ORDER");
        assertTrue(id > 0L);
    }


    /**
     * 执行 nextId_returns_increasing_values 方法。
     */
    public void nextId_returns_increasing_values() {
        IdServiceImpl service = new IdServiceImpl("1", "1");
        long a = service.nextId("ORDER");
        long b = service.nextId("ORDER");
        long c = service.nextId("ORDER");
        assertTrue(b > a);
        assertTrue(c > b);
    }


    /**
     * 执行 nextId_ignores_bizTag_and_keeps_increasing 方法。
     */
    public void nextId_ignores_bizTag_and_keeps_increasing() {
        IdServiceImpl service = new IdServiceImpl("1", "1");
        long a = service.nextId("ORDER");
        long b = service.nextId("USER");
        long c = service.nextId("PAY");
        assertTrue(b > a);
        assertTrue(c > b);
        assertNotEquals(a, b);
        assertNotEquals(b, c);
    }


    /**
     * 执行 nextId_handles_null_bizTag 方法。
     */
    public void nextId_handles_null_bizTag() {
        IdServiceImpl service = new IdServiceImpl("1", "1");
        long id = service.nextId(null);
        assertTrue(id > 0L);
    }


    /**
     * 执行 nextId_consecutive_calls_distinct_and_positive 方法。
     */
    public void nextId_consecutive_calls_distinct_and_positive() {
        IdServiceImpl service = new IdServiceImpl("1", "1");
        long a = service.nextId("ORDER");
        long b = service.nextId("ORDER");
        assertTrue(a > 0L);
        assertTrue(b > 0L);
        assertNotEquals(a, b);
    }


    /**
     * 执行 constructor_missing_node_id_throws 方法。
     */
    public void constructor_missing_node_id_throws() {
        assertThrows(IllegalStateException.class, () -> new IdServiceImpl("", "1"));
        assertThrows(IllegalStateException.class, () -> new IdServiceImpl("1", ""));
    }


    /**
     * 执行 constructor_out_of_range_node_id_throws 方法。
     */
    public void constructor_out_of_range_node_id_throws() {
        assertThrows(IllegalArgumentException.class, () -> new IdServiceImpl("32", "1"));
        assertThrows(IllegalArgumentException.class, () -> new IdServiceImpl("1", "32"));
    }
}
