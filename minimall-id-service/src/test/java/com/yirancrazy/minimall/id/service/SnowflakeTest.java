package com.yirancrazy.minimall.id.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 独立 Snowflake 生成器的单元测试。覆盖单节点序列单调性和生成 long 值的
 *     符号；本测试类未覆盖时钟回拨和完整位布局边界。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class SnowflakeTest {

  /** 验证单节点序列递增：Snowflake(1,1)连续生成1000个ID，预期每次结果均大于前一个ID. */
  @Test
  void testGeneratedIdsAreMonotonicAndUnique() {
    Snowflake sf = new Snowflake(1L, 1L);
    long prev = 0L;
    for (int i = 0; i < 1000; i++) {
      long id = sf.nextId();
      assertTrue(id > prev, "id should increase: " + id + " after " + prev);
      prev = id;
    }
  }

  /** 验证生成ID为正long：Snowflake(7,7)生成首个ID，预期大于0；本用例不验证具体位段. */
  @Test
  void testIdKeepsTopBitsConsistent() {
    Snowflake sf = new Snowflake(7L, 7L);
    long id = sf.nextId();
    assertTrue(id > 0);
  }
}
