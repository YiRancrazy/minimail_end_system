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
 * @Description: 验证进程内事件总线向 Spring 事件监听器发布事件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class LocalEventBusTest {

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试事件，携带字符串载荷。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public static class SampleEvent {
        private final String payload;

        /**
         * 创建测试事件。
         *
         * @param p 事件载荷
         */
        public SampleEvent(String p) {
            this.payload = p;
        }

        /**
         * 获取事件载荷。
         *
         * @return 字符串载荷
         */
        public String getPayload() {
            return payload;
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 记录收到事件的测试监听器。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @Component
    public static class Recorder {
        public final List<String> received = new ArrayList<>();
        public final AtomicInteger calls = new AtomicInteger();

        /**
         * 记录事件并累加调用次数。
         *
         * @param e 收到的事件
         */
        @EventListener
        public void onSample(SampleEvent e) {
            received.add(e.getPayload());
            calls.incrementAndGet();
        }
    }


    /**
     * 发布事件时应调用 Spring 事件监听器。
     */
    @Test
    public void publish_invokes_event_listener() {
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


    /**
     * 携带显式路由发布时仍应正常触发本地监听器。
     */
    @Test
    public void publishWithRoute_publishesEventOnce() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(LocalEventBus.class, Recorder.class);
        ctx.refresh();

        LocalEventBus bus = ctx.getBean(LocalEventBus.class);
        Recorder rec = ctx.getBean(Recorder.class);

        bus.publish("outbox-topic", "outbox-tag", new SampleEvent("hello"));

        assertEquals(1, rec.calls.get());
        assertEquals(List.of("hello"), rec.received);
        ctx.close();
    }
}
