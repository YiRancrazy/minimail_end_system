package com.yirancrazy.minimall.pay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SeataParticipantWiringTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class SeataParticipantWiringTest {

    @Test
    void seataClientClassesAvailable() throws Exception {
        assertNotNull(Class.forName("io.seata.spring.annotation.GlobalTransactional"));
        assertNotNull(Class.forName("io.seata.rm.datasource.DataSourceProxy"));
    }
}
