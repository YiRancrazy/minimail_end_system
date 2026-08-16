package com.yirancrazy.minimall.notify.sse;

import org.junit.jupiter.api.Test;
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
}
