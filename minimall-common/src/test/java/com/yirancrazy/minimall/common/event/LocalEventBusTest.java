package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocalEventBusTest {

    public static class SampleEvent {
        private final String payload;
        public SampleEvent(String p) { this.payload = p; }
        public String getPayload() { return payload; }
    }

    @Component
    public static class Recorder {
        public final List<String> received = new ArrayList<>();
        public final AtomicInteger calls = new AtomicInteger();
        @EventListener
        public void onSample(SampleEvent e) {
            received.add(e.getPayload());
            calls.incrementAndGet();
        }
    }

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
}