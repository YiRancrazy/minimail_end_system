package com.yirancrazy.minimall.notify.sse;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseHubTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class SseHubTest {

    private final SseHub sseHub = new SseHub();


    @Test
    void open_rejects_subscribe_other_user() {
        BizException ex = assertThrows(BizException.class, () -> sseHub.register(1L, 2L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN.getCode(), ex.getCode());
    }


    @Test
    void open_accepts_matching_user() {
        assertNotNull(sseHub.register(1L, 1L));
    }


    @Test
    void open_accepts_header_user_without_param() {
        assertNotNull(sseHub.register(1L, null));
    }


    @Test
    void register_rejects_when_connection_limit_reached() {
        for (int i = 0; i < 5; i++) {
            sseHub.register(1L, null);
        }
        BizException ex = assertThrows(BizException.class, () -> sseHub.register(1L, null));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());
    }


    @Test
    void register_rolls_back_when_ready_send_fails() {
        SseHub hub = new ReadyFailingSseHub();

        for (int i = 0; i < 8; i++) {
            BizException ex = assertThrows(BizException.class, () -> hub.register(1L));
            assertEquals(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED.getCode(), ex.getCode());
        }

        // 配额已随回滚释放，第 9 次仍是连接建立失败而非连接数超限
        BizException ex = assertThrows(BizException.class, () -> hub.register(1L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED.getCode(), ex.getCode());
    }


    @Test
    void send_removes_connection_and_releases_quota_on_any_exception() {
        SseHub hub = new NotifyFailingSseHub();
        assertNotNull(hub.register(1L));

        hub.send(1L, "payload");

        // 失效连接已移除：配额释放后可重新占满 5 条连接
        for (int i = 0; i < 5; i++) {
            assertNotNull(hub.register(1L));
        }
        BizException ex = assertThrows(BizException.class, () -> hub.register(1L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());
    }


    @Test
    void global_limit_rejects_new_user_and_releases_after_completion() {
        SseHub hub = new CompletingSseHub(1, 30_000L, new CountDownLatch(1));
        CompletingSseEmitter first = (CompletingSseEmitter) hub.register(1L);

        BizException ex = assertThrows(BizException.class, () -> hub.register(2L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());

        first.complete();
        assertNotNull(hub.register(2L));
    }


    @Test
    void idle_connection_is_reclaimed_after_timeout() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);
        SseHub hub = new CompletingSseHub(1, 50L, completionLatch);
        assertNotNull(hub.register(1L));

        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(hub.register(2L));
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: ReadyFailingSseHub 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private static final class ReadyFailingSseHub extends SseHub {

        /**
         * 创建 SSE 发射器。
         * @return SSE 发射器
         */
        @Override
        SseEmitter createEmitter() {
            return new SseEmitter(0L) {
                public void send(SseEventBuilder builder) throws IOException {
                    throw new IOException("client gone before ready");
                }
            };
        }
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: NotifyFailingSseHub 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private static final class NotifyFailingSseHub extends SseHub {

        @Override
        SseEmitter createEmitter() {
            return new SseEmitter(0L) {
                private int sendCount;

                /**
                 * 发送业务消息。
                 * @param builder builder 参数
                 * @throws IOException 处理失败时抛出
                 */
                public void send(SseEventBuilder builder) throws IOException {
                    sendCount++;
                    if (sendCount > 1) {
                        throw new IllegalStateException("emitter already completed");
                    }
                    super.send(builder);
                }
            };
        }
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: CompletingSseHub 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private static final class CompletingSseHub extends SseHub {

        private final CountDownLatch completionLatch;

        CompletingSseHub(int maxGlobalConnections, long connectionTimeoutMillis, CountDownLatch completionLatch) {
            super(maxGlobalConnections, connectionTimeoutMillis);
            this.completionLatch = completionLatch;
        }

        @Override
        SseEmitter createEmitter() {
            return new CompletingSseEmitter(completionLatch);
        }
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: CompletingSseEmitter 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    private static final class CompletingSseEmitter extends SseEmitter {

        private final CountDownLatch completionLatch;

        private Runnable completionCallback;

        CompletingSseEmitter(CountDownLatch completionLatch) {
            super(0L);
            this.completionLatch = completionLatch;
        }

        /**
         * 执行 onCompletion 方法。
         * @param callback callback 参数
         */
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
            super.onCompletion(callback);
        }

        /**
         * 执行 complete 方法。
         */
        public synchronized void complete() {
            super.complete();
            if (completionCallback != null) {
                completionCallback.run();
            }
            completionLatch.countDown();
        }
    }
}
