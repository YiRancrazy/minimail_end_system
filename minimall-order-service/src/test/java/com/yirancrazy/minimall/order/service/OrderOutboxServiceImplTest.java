package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.impl.OrderOutboxServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱服务单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
class OrderOutboxServiceImplTest {

    private OutboxManager outboxManager;
    private OrderOutboxService orderOutboxService;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        outboxManager = mock(OutboxManager.class);
        orderOutboxService = new OrderOutboxServiceImpl(outboxManager, "minimall-events");
    }

    /**
     * 验证发件箱保存返回 false 时抛出明确的业务异常。
     */
    @Test
    void recordPaidEvent_throwsWhenSaveReturnsFalse() {
        when(outboxManager.save(any(OutboxPO.class))).thenReturn(false);

        BizException exception = assertThrows(BizException.class,
            () -> orderOutboxService.recordPaidEvent(paidEvent()));

        assertEquals(OrderCodeEnum.ORDER_OUTBOX_SAVE_FAILED.getCode(), exception.getCode());
        assertEquals(OrderCodeEnum.ORDER_OUTBOX_SAVE_FAILED.getMessage(), exception.getMessage());
    }

    /**
     * 构造订单支付成功事件。
     *
     * @return 订单支付成功事件
     */
    private OrderPaidDTO paidEvent() {
        return new OrderPaidDTO(
            99L, 7L, new BigDecimal("10.00"), LocalDateTime.now().toString());
    }
}
