package com.yirancrazy.minimall.pay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付服务具备 Seata 全局事务参与方运行依赖。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 */
class SeataParticipantWiringTest {

    @Test
    void seataClientClassesAvailable() throws Exception {
        assertNotNull(Class.forName("io.seata.spring.annotation.GlobalTransactional"));
        assertNotNull(Class.forName("io.seata.rm.datasource.DataSourceProxy"));
    }
}
