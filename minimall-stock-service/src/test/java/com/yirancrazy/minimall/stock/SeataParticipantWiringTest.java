package com.yirancrazy.minimall.stock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过类路径加载检查库存服务测试运行时是否具备 Seata 核心依赖。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class SeataParticipantWiringTest {

    /** * 目标验证 Seata 依赖；前置为测试类路径含相关包；步骤用 Class.forName 加载两类；预期均非空. */
    @Test
    void testSeataClientClassesAvailable() throws Exception {
        assertNotNull(Class.forName("io.seata.spring.annotation.GlobalTransactional"));
        assertNotNull(Class.forName("io.seata.rm.datasource.DataSourceProxy"));
    }
}
