package com.yirancrazy.minimall.pay;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付模块运行时具备 Seata 参与者所需类。
 * 通过类加载检查全局事务注解和数据源代理依赖，不建立事务或数据库连接。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class SeataParticipantWiringTest {

    /** 验证 Seata 依赖：加载注解与代理类，预期类均可用且不建立事务. */
    @Test
    void testSeataClientClassesAvailable() throws Exception {
        assertNotNull(Class.forName("io.seata.spring.annotation.GlobalTransactional"));
        assertNotNull(Class.forName("io.seata.rm.datasource.DataSourceProxy"));
    }
}
