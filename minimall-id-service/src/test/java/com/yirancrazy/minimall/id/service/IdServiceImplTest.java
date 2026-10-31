package com.yirancrazy.minimall.id.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yirancrazy.minimall.id.service.impl.IdServiceImpl;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Snowflake 版 IdService 的单元测试。不依赖 Spring 或数据库，覆盖有效发号、
 *     递增顺序、业务标签独立性、配置缺失和节点编号越界失败。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class IdServiceImplTest {

  /** 验证有效节点可生成正数ID：前置节点为(1,1)，请求ORDER一次，预期返回的long大于0. */
  @Test
  public void testNextIdReturnsPositiveValue() {
    IdServiceImpl service = new IdServiceImpl("1", "1");
    long id = service.nextId("ORDER");
    assertTrue(id > 0L);
  }

  /** 验证单实例连号递增：前置节点为(1,1)，连续生成三个ORDER ID，预期后值均大于前值. */
  @Test
  public void testNextIdReturnsIncreasingValues() {
    IdServiceImpl service = new IdServiceImpl("1", "1");
    long a = service.nextId("ORDER");
    long b = service.nextId("ORDER");
    long c = service.nextId("ORDER");
    assertTrue(b > a);
    assertTrue(c > b);
  }

  /** 验证业务标签不重置序列：同一实例依次请求ORDER、USER、PAY，预期严格递增且两两不同. */
  @Test
  public void testNextIdIgnoresBizTagAndKeepsIncreasing() {
    IdServiceImpl service = new IdServiceImpl("1", "1");
    long a = service.nextId("ORDER");
    long b = service.nextId("USER");
    long c = service.nextId("PAY");
    assertTrue(b > a);
    assertTrue(c > b);
    assertNotEquals(a, b);
    assertNotEquals(b, c);
  }

  /** 验证空业务标签可发号：前置节点为(1,1)，以null请求一次，预期仍返回正数ID. */
  @Test
  public void testNextIdHandlesNullBizTag() {
    IdServiceImpl service = new IdServiceImpl("1", "1");
    long id = service.nextId(null);
    assertTrue(id > 0L);
  }

  /** 验证连续调用不重复：同一实例连续请求两个ORDER ID，预期两者均为正数且互不相同. */
  @Test
  public void testNextIdConsecutiveCallsDistinctAndPositive() {
    IdServiceImpl service = new IdServiceImpl("1", "1");
    long a = service.nextId("ORDER");
    long b = service.nextId("ORDER");
    assertTrue(a > 0L);
    assertTrue(b > 0L);
    assertNotEquals(a, b);
  }

  /** 验证节点配置缺失即失败：分别构造datacenter或worker为空的服务，预期抛出IllegalStateException. */
  @Test
  public void testConstructorMissingNodeIdThrows() {
    assertThrows(IllegalStateException.class, () -> new IdServiceImpl("", "1"));
    assertThrows(IllegalStateException.class, () -> new IdServiceImpl("1", ""));
  }

  /** 验证节点编号越界即失败：分别将datacenter或worker设为32，预期构造时抛出IllegalArgumentException. */
  @Test
  public void testConstructorOutOfRangeNodeIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> new IdServiceImpl("32", "1"));
    assertThrows(IllegalArgumentException.class, () -> new IdServiceImpl("1", "32"));
  }
}
