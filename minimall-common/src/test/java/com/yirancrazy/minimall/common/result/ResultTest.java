package com.yirancrazy.minimall.common.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证统一响应 Result 与 PageResult 的字段填充规则，尤其是 MDC traceId 传递。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class ResultTest {

  /** * 验证成功响应；传入数据hi；读取码消息数据；预期码为成功、消息null、数据保留. */
  @Test
  void testSuccessHasCodeZeroAndData() {
    Result<String> r = Result.success("hi");
    assertEquals("00000", r.getCode());
    assertNull(r.getMessage());
    assertEquals("hi", r.getData());
  }

  /** * 验证失败响应；传入错误码和未授权；读取码消息数据；预期码消息保留、数据null. */
  @Test
  void testFailKeepsCodeAndMessage() {
    Result<Void> r = Result.fail("14001", "未授权");
    assertEquals("14001", r.getCode());
    assertEquals("未授权", r.getMessage());
    assertNull(r.getData());
  }

  /** * 验证成功取traceId；MDC预置tid-123；创建成功响应；预期响应保留该值. */
  @Test
  void testSuccessFillsTraceIdFromMdc() {
    MDC.put(Result.TRACE_ID_KEY, "tid-123");
    try {
      Result<String> r = Result.success("hi");
      assertEquals("tid-123", r.getTraceId());
    } finally {
      MDC.clear();
    }
  }

  /** * 验证失败取traceId；MDC预置tid-456；创建失败响应；预期响应保留该值. */
  @Test
  void testFailKeepsTraceIdFromMdc() {
    MDC.put(Result.TRACE_ID_KEY, "tid-456");
    try {
      Result<Void> r = Result.fail("14001", "未授权");
      assertEquals("tid-456", r.getTraceId());
    } finally {
      MDC.clear();
    }
  }

  /** * 验证分页包装；构造总数为1且单记录；读取总数记录；预期总数和大小均为1. */
  @Test
  void testPageResultWrapsRecords() {
    PageResult<String> p = new PageResult<>(1L, 10L, 10L, 1L, List.of("a"));
    assertEquals(1L, p.getTotal());
    assertEquals(1, p.getRecords().size());
  }
}
