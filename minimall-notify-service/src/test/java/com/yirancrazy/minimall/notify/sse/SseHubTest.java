package com.yirancrazy.minimall.notify.sse;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.notify.constant.NotifyCodeEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SseHub 单元测试，验证 SSE 订阅归属校验与单用户连接数限流。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
class SseHubTest {

    private final SseHub sseHub = new SseHub();

    /**
     * 验证订阅他人 userId 时被拒绝：请求参数与 X-User-Id 不一致抛 FORBIDDEN。
     */
    @Test
    void open_rejects_subscribe_other_user() {
        BizException ex = assertThrows(BizException.class, () -> sseHub.open(1L, 2L));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_FORBIDDEN.getCode(), ex.getCode());
    }

    /**
     * 验证参数 userId 与 X-User-Id 一致时正常建立连接。
     */
    @Test
    void open_accepts_matching_user() {
        assertNotNull(sseHub.open(1L, 1L));
    }

    /**
     * 验证不传 userId 参数时以 X-User-Id 为准正常建立连接。
     */
    @Test
    void open_accepts_header_user_without_param() {
        assertNotNull(sseHub.open(1L, null));
    }

    /**
     * 验证同一用户连接数超过上限后新连接被拒绝，防止连接耗尽型 DoS。
     */
    @Test
    void register_rejects_when_connection_limit_reached() {
        for (int i = 0; i < 5; i++) {
            sseHub.open(1L, null);
        }
        BizException ex = assertThrows(BizException.class, () -> sseHub.open(1L, null));
        assertEquals(NotifyCodeEnum.NOTIFY_SSE_LIMIT.getCode(), ex.getCode());
    }

    /**
     * 验证首次 ready 发送失败时回滚注册并释放配额：连续失败不会累积占用连接数。
     */
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

    /**
     * 验证推送时任意异常（含非 IO 异常）都会移除连接并释放配额。
     */
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

    /**
     * ready 事件发送失败的可控 SSE Hub：用于验证注册回滚。
     */
    private static final class ReadyFailingSseHub extends SseHub {

        @Override
        SseEmitter createEmitter() {
            return new SseEmitter(0L) {
                @Override
                public void send(SseEventBuilder builder) throws IOException {
                    throw new IOException("client gone before ready");
                }
            };
        }
    }

    /**
     * 首次（ready）发送成功、后续（notify）发送抛非 IO 异常的可控 SSE Hub，模拟连接已完成的僵尸连接。
     */
    private static final class NotifyFailingSseHub extends SseHub {

        @Override
        SseEmitter createEmitter() {
            return new SseEmitter(0L) {
                private int sendCount;

                @Override
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
}
