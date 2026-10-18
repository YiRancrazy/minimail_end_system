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
 * @Description: LocalEventBusTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class LocalEventBusTest {

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: SampleEvent 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public static class SampleEvent {
        private final String payload;
        /**
         * 构造 SampleEvent 实例。
         * @param p p 参数
         */
        public SampleEvent(String p) {
            this.payload = p;
        }

        /**
         * 获取相关数据。
         * @return 处理结果
         */
        public String getPayload() {
            return payload;
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: Recorder 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @Component
    public static class Recorder {
        public final List<String> received = new ArrayList<>();
        public final AtomicInteger calls = new AtomicInteger();

        /**
         * 执行 onSample 方法。
         * @param e e 参数
         */
        public void onSample(SampleEvent e) {
            received.add(e.getPayload());
            calls.incrementAndGet();
        }
    }


    /**
     * 执行 publish_invokes_event_listener 方法。
     */
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
}