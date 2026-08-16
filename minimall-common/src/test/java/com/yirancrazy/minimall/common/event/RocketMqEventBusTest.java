package com.yirancrazy.minimall.common.event;

import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RocketMqEventBus traceId 透传的单元测试类，验证生产者将 MDC traceId 写入消息属性。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
class RocketMqEventBusTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void attachTraceId_givenMdcValue_thenMessagePropertySet() {
        MDC.put(Result.TRACE_ID_KEY, "tid-xyz");
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertEquals("tid-xyz", msg.getUserProperty(TraceIdFilter.HEADER));
    }

    @Test
    void attachTraceId_givenNoMdcValue_thenNoPropertySet() {
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertNull(msg.getUserProperty(TraceIdFilter.HEADER));
    }
}
