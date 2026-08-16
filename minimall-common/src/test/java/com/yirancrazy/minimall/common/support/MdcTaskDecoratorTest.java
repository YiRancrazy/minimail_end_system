package com.yirancrazy.minimall.common.support;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MdcTaskDecorator 的单元测试类，验证 MDC（含 traceId）在提交线程与执行线程间的透传与还原。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
class MdcTaskDecoratorTest {

    private MdcTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new MdcTaskDecorator();
        MDC.put("traceId", "tid-abc");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void decorate_givenMdcContext_thenCopiedToWorkerThread() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> seen.set(MDC.get("traceId")));

        Thread worker = new Thread(decorated);
        worker.start();
        worker.join();

        assertEquals("tid-abc", seen.get());
    }

    @Test
    void decorate_whenNoSubmitContext_thenWorkerMdcCleared() throws Exception {
        MDC.clear();
        AtomicReference<String> seen = new AtomicReference<>("keep");
        Runnable decorated = decorator.decorate(() -> seen.set(MDC.get("traceId")));

        Thread worker = new Thread(decorated);
        worker.start();
        worker.join();

        assertNull(seen.get());
    }

    @Test
    void decorate_thenRestoresWorkerOriginalContext() throws Exception {
        AtomicReference<String> after = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> {
        });

        Thread worker = new Thread(() -> {
            MDC.put("traceId", "worker-tid");
            decorated.run();
            after.set(MDC.get("traceId"));
        });
        worker.start();
        worker.join();

        assertEquals("worker-tid", after.get());
    }
}
