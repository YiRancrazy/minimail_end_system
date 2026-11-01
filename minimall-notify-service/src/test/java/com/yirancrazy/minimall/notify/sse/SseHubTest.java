package com.yirancrazy.minimall.notify.sse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SSE 订阅身份、单用户与全局连接配额、ready 失败回滚、发送失败回收和空闲超时清理行为.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class SseHubTest {

    private final SseHub sseHub = new SseHub();

    /** 验证请求头用户1与查询参数用户2不一致时抛出订阅越权业务错误. */
    @Test
    void testOpenRejectsSubscribeOtherUser() {
        BizException ex = assertThrows(BizException.class, () -> sseHub.register(1L, 2L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN.getCode(), ex.getCode());
    }

    /** 验证请求头和查询参数用户均为1时返回非空SSE发射器. */
    @Test
    void testOpenAcceptsMatchingUser() {
        assertNotNull(sseHub.register(1L, 1L));
    }

    /** 验证未提供查询参数用户时使用请求头用户1完成订阅. */
    @Test
    void testOpenAcceptsHeaderUserWithoutParam() {
        assertNotNull(sseHub.register(1L, null));
    }

    /** 验证同一用户已有5条连接时第6次注册抛出连接数超限业务错误. */
    @Test
    void testRegisterRejectsWhenConnectionLimitReached() {
        for (int i = 0; i < 5; i++) {
            sseHub.register(1L, null);
        }
        BizException ex = assertThrows(BizException.class, () -> sseHub.register(1L, null));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());
    }

    /** 验证ready事件持续发送失败时每次均报连接失败而非连接数超限. */
    @Test
    void testRegisterRollsBackWhenReadySendFails() {
        SseHub hub = new ReadyFailingSseHub();

        for (int i = 0; i < 8; i++) {
            BizException ex = assertThrows(BizException.class, () -> hub.register(1L));
            assertEquals(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED.getCode(), ex.getCode());
        }

        // 配额已随回滚释放，第 9 次仍是连接建立失败而非连接数超限
        BizException ex = assertThrows(BizException.class, () -> hub.register(1L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_CONNECT_FAILED.getCode(), ex.getCode());
    }

    /** 验证通知发送异常会移除失效连接并释放配额供新连接使用. */
    @Test
    void testSendRemovesConnectionAndReleasesQuotaOnAnyException() {
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

    /** 验证全局上限为1时用户2先被拒绝，用户1完成后用户2可注册. */
    @Test
    void testGlobalLimitRejectsNewUserAndReleasesAfterCompletion() {
        SseHub hub = new CompletingSseHub(1, 30_000L, new CountDownLatch(1));
        CompletingSseEmitter first = (CompletingSseEmitter) hub.register(1L);

        BizException ex = assertThrows(BizException.class, () -> hub.register(2L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());

        first.complete();
        assertNotNull(hub.register(2L));
    }

    /** 验证连接空闲超时后自动完成并释放全局配额供其他用户使用. */
    @Test
    void testIdleConnectionIsReclaimedAfterTimeout() throws InterruptedException {
        CountDownLatch completionLatch = new CountDownLatch(1);
        SseHub hub = new CompletingSseHub(1, 50L, completionLatch);
        assertNotNull(hub.register(1L));

        assertTrue(completionLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(hub.register(2L));
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试替身，创建在 ready 事件发送时固定抛出 IOException 的发射器，用于验证注册回滚.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    private static final class ReadyFailingSseHub extends SseHub {

        /**
         * 创建 ready 事件必然发送失败的 SSE 发射器，以触发 SseHub 的注册回滚分支.
         *
         * @return 发送 ready 时抛出 IOException 的发射器
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
     * @Description: 测试替身，创建在第二次及后续 send 调用时抛出异常的发射器，用于验证失效连接回收.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    private static final class NotifyFailingSseHub extends SseHub {

        /** 创建允许 ready 成功、notify 第二次发送失败的 SSE 发射器. */
        @Override
        SseEmitter createEmitter() {
            return new SseEmitter(0L) {
                private int sendCount;

                /**
                 * 统计发送次数；第二次及后续发送抛出异常，模拟连接已失效.
                 *
                 * @param builder 待发送的 SSE 事件
                 * @throws IOException 当前测试替身未主动抛出该异常，保留接口声明
                 * @throws IllegalStateException 第二次及后续发送时抛出
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
     * @Description: 测试替身，使用可观测完成的发射器和闩锁验证连接完成、超时清理及全局配额释放.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    private static final class CompletingSseHub extends SseHub {

        private final CountDownLatch completionLatch;

        /** 保存完成闩锁，并以指定全局上限和超时时间初始化 SseHub. */
        CompletingSseHub(
                int maxGlobalConnections, long connectionTimeoutMillis, CountDownLatch completionLatch) {
            super(maxGlobalConnections, connectionTimeoutMillis);
            this.completionLatch = completionLatch;
        }

        /** 创建携带当前闩锁的 CompletingSseEmitter，供测试观测完成事件. */
        @Override
        SseEmitter createEmitter() {
            return new CompletingSseEmitter(completionLatch);
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试替身，在 complete 时执行回调并释放 CountDownLatch，供验证空闲回收和连接完成.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    private static final class CompletingSseEmitter extends SseEmitter {

        private final CountDownLatch completionLatch;

        private Runnable completionCallback;

        /** 绑定完成闩锁并创建无固定超时的 SSE 发射器. */
        CompletingSseEmitter(CountDownLatch completionLatch) {
            super(0L);
            this.completionLatch = completionLatch;
        }

        /**
         * 保存完成回调并注册到父类，供测试替身的 complete 方法主动触发.
         *
         * @param callback 连接完成后执行的回调
         */
        public synchronized void onCompletion(Runnable callback) {
            this.completionCallback = callback;
            super.onCompletion(callback);
        }

        /** 完成父类发射器，执行已注册回调并释放完成闩锁. */
        public synchronized void complete() {
            super.complete();
            if (completionCallback != null) {
                completionCallback.run();
            }
            completionLatch.countDown();
        }
    }
}
