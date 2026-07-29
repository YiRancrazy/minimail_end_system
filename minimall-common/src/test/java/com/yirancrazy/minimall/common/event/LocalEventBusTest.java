package com.yirancrazy.minimall.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 本地事件总线单元测试，验证事件发布后能被 @EventListener 监听方法按序接收，并校验调用次数与载荷内容。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public class LocalEventBusTest
{

    public static class SampleEvent
    {
        private final String payload;
        public SampleEvent(String p)
        {
            this.payload = p;
        }
        /**
         * 获取测试事件携带的字符串载荷。
         *
         * @return 构造时传入的载荷内容
         */
        public String getPayload()
        {
            return payload;
        }
    }

    @Component
    public static class Recorder
    {
        public final List<String> received = new ArrayList<>();
        public final AtomicInteger calls = new AtomicInteger();
        /**
         * 监听测试事件并记录载荷与调用次数，供断言校验。
         *
         * @param e 由事件总线发布的测试事件
         */
        @EventListener
        public void onSample(SampleEvent e)
        {
            received.add(e.getPayload());
            calls.incrementAndGet();
        }
    }

    /**
     * 验证事件总线发布事件后，监听方法被按序调用且接收到正确载荷。
     */
    @Test
    public void publish_invokes_event_listener()
    {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LocalEventBus.class, Recorder.class);
        ctx.refresh();

        LocalEventBus bus = ctx.getBean(LocalEventBus.class);
        Recorder rec = ctx.getBean(Recorder.class);

        bus.publish(new SampleEvent("hello"));
        bus.publish(new SampleEvent("world"));

        assertEquals(2, rec.calls.get());
        assertEquals(List.of("hello", "world"), rec.received);
        ctx.close();
    }
}