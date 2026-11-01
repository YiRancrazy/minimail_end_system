package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.impl.OrderOutboxServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证发件箱服务在保存失败时的业务异常语义。
 *   业务边界：使用 OutboxManager 桩构造 save 返回 false，不开启真实事务。
 *   使用场景：覆盖 ORDER_OUTBOX_SAVE_FAILED 错误码与消息。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
class OrderOutboxServiceImplTest {

    private OutboxManager outboxManager;
    private OrderOutboxService orderOutboxService;

    /**
     * 初始化当前测试类的共享夹具。
     *
     * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
     */
    @BeforeEach
    void setUp() {
        outboxManager = mock(OutboxManager.class);
        orderOutboxService = new OrderOutboxServiceImpl(outboxManager, "minimall-events");
    }

    /** 验证发件箱保存返回 false 时抛出业务异常. */
    @Test
    void testRecordPaidEventThrowsWhenSaveReturnsFalse() {
        when(outboxManager.save(any(OutboxPo.class))).thenReturn(false);

        BizException exception =
                assertThrows(BizException.class, () -> orderOutboxService.recordPaidEvent(paidEvent()));

        assertEquals(OrderCodeEnum.ORDER_OUTBOX_SAVE_FAILED.getCode(), exception.getCode());
        assertEquals(OrderCodeEnum.ORDER_OUTBOX_SAVE_FAILED.getMessage(), exception.getMessage());
    }

    /**
     * 构造订单支付成功事件.
     *
     * @return 订单支付成功事件
     */
    private OrderPaidDto paidEvent() {
        return new OrderPaidDto(99L, 7L, new BigDecimal("10.00"), LocalDateTime.now().toString());
    }
}
