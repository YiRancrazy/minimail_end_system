package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.service.impl.OrderCheckoutServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证结算来源分流、服务端购物车快照和清理异常隔离。
 *   业务边界：使用 OrderService、CartCleanupService、OrderRemoteGateway 三个 Mockito 桩。
 *   使用场景：覆盖购物车、立即购买、清理异常和缺失来源场景。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class OrderCheckoutServiceImplTest {

    private OrderService orderService;
    private CartCleanupService cartCleanupService;
    private OrderRemoteGateway orderRemoteGateway;
    private OrderCheckoutServiceImpl service;

    /**
     * 初始化当前测试类的共享夹具。
     *
     * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
     */
    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        cartCleanupService = mock(CartCleanupService.class);
        orderRemoteGateway = mock(OrderRemoteGateway.class);
        service = new OrderCheckoutServiceImpl(orderService, cartCleanupService, orderRemoteGateway);
    }

    /** 验证结算购物车来源使用服务端快照. */
    @Test
    void testCheckoutCartSourceUsesServerSnapshot() {
        OrderCheckoutDto dto = cartCheckoutDto(List.of(9001L));
        List<CartCheckoutItemDto> cleanupItems = List.of(new CartCheckoutItemDto(9001L, 100L, 2));
        when(orderRemoteGateway.cartCheckoutSnapshot(any())).thenReturn(Result.success(cleanupItems));
        when(orderService.checkout(eq(7L), any(), eq(cleanupItems), any()))
                .thenReturn(new CheckoutResultDto(100L, "OG1", 200L));

        assertEquals(100L, service.checkout(7L, dto));

        verify(orderRemoteGateway).cartCheckoutSnapshot(any());
        verify(orderService).checkout(eq(7L), any(), eq(cleanupItems), any());
        verify(cartCleanupService).tryCleanup(200L);
    }

    /** 验证立即购买结算不请求购物车快照. */
    @Test
    void testCheckoutBuyNowDoesNotRequestCartSnapshot() {
        OrderCheckoutDto dto = buyNowDto();
        when(orderService.checkout(eq(7L), any(), eq(List.of()), any()))
                .thenReturn(new CheckoutResultDto(100L, "OG1", null));

        assertEquals(100L, service.checkout(7L, dto));

        verify(orderRemoteGateway, never()).cartCheckoutSnapshot(any());
        verify(cartCleanupService, never()).tryCleanup(any());
    }

    /** 验证购物车清理异常不影响订单创建结果. */
    @Test
    void testCheckoutCleanupExceptionDoesNotFailOrder() {
        final OrderCheckoutDto dto = cartCheckoutDto(List.of(9001L));
        List<CartCheckoutItemDto> cleanupItems = List.of(new CartCheckoutItemDto(9001L, 100L, 2));
        when(orderRemoteGateway.cartCheckoutSnapshot(any())).thenReturn(Result.success(cleanupItems));
        when(orderService.checkout(eq(7L), any(), eq(cleanupItems), any()))
                .thenReturn(new CheckoutResultDto(100L, "OG1", 200L));
        when(cartCleanupService.tryCleanup(200L))
                .thenThrow(new IllegalStateException("cleanup failed"));

        assertEquals(100L, service.checkout(7L, dto));
    }

    /** 验证结算缺失来源抛出参数无效. */
    @Test
    void testCheckoutMissingSourceThrowsParameterInvalid() {
        BizException exception =
                assertThrows(BizException.class, () -> service.checkout(7L, new OrderCheckoutDto()));

        assertEquals(CommonCode.PARAM_INVALID, exception.getCode());
    }

    /**
     * 构造购物车来源结算请求。
     *
     * <p>设置 CART 来源和指定购物车项 ID，不设置立即购买 items。
     *
     * @param ids 购物车项 ID 列表
     * @return 购物车结算请求。
     */
    private OrderCheckoutDto cartCheckoutDto(List<Long> ids) {
        OrderCheckoutDto dto = new OrderCheckoutDto();
        dto.setCheckoutSource(CheckoutSourceEnum.CART);
        dto.setCartItemIds(ids);
        return dto;
    }

    /**
     * 构造立即购买结算请求。
     *
     * <p>设置 BUY_NOW 来源和单个 SKU 明细，不携带购物车项 ID。
     *
     * @return 立即购买请求。
     */
    private OrderCheckoutDto buyNowDto() {
        OrderCheckoutDto dto = new OrderCheckoutDto();
        dto.setCheckoutSource(CheckoutSourceEnum.BUY_NOW);
        dto.setItems(List.of(new OrderCheckoutItemDto(100L, 1)));
        return dto;
    }
}
