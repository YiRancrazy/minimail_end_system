package com.yirancrazy.minimall.order.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDTO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDTO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.service.impl.OrderCheckoutServiceImpl;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算编排服务测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class OrderCheckoutServiceImplTest {

    private OrderService orderService;
    private CartCleanupService cartCleanupService;
    private OrderRemoteGateway orderRemoteGateway;
    private OrderCheckoutServiceImpl service;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        cartCleanupService = mock(CartCleanupService.class);
        orderRemoteGateway = mock(OrderRemoteGateway.class);
        service = new OrderCheckoutServiceImpl(
            orderService, cartCleanupService, orderRemoteGateway);
    }

    @Test
    void checkout_cartSource_usesServerSnapshot() {
        OrderCheckoutDTO dto = cartCheckoutDto(List.of(9001L));
        List<CartCheckoutItemDTO> cleanupItems =
            List.of(new CartCheckoutItemDTO(9001L, 100L, 2));
        when(orderRemoteGateway.cartCheckoutSnapshot(any()))
            .thenReturn(Result.success(cleanupItems));
        when(orderService.checkout(eq(7L), any(), eq(cleanupItems), any()))
            .thenReturn(new CheckoutResultDTO(100L, "OG1", 200L));

        assertEquals(100L, service.checkout(7L, dto));

        verify(orderRemoteGateway).cartCheckoutSnapshot(any());
        verify(orderService).checkout(eq(7L), any(), eq(cleanupItems), any());
        verify(cartCleanupService).tryCleanup(200L);
    }

    @Test
    void checkout_buyNow_doesNotRequestCartSnapshot() {
        OrderCheckoutDTO dto = buyNowDto();
        when(orderService.checkout(eq(7L), any(), eq(List.of()), any()))
            .thenReturn(new CheckoutResultDTO(100L, "OG1", null));

        assertEquals(100L, service.checkout(7L, dto));

        verify(orderRemoteGateway, never()).cartCheckoutSnapshot(any());
        verify(cartCleanupService, never()).tryCleanup(any());
    }

    @Test
    void checkout_cleanupExceptionDoesNotFailOrder() {
        OrderCheckoutDTO dto = cartCheckoutDto(List.of(9001L));
        List<CartCheckoutItemDTO> cleanupItems =
            List.of(new CartCheckoutItemDTO(9001L, 100L, 2));
        when(orderRemoteGateway.cartCheckoutSnapshot(any()))
            .thenReturn(Result.success(cleanupItems));
        when(orderService.checkout(eq(7L), any(), eq(cleanupItems), any()))
            .thenReturn(new CheckoutResultDTO(100L, "OG1", 200L));
        when(cartCleanupService.tryCleanup(200L))
            .thenThrow(new IllegalStateException("cleanup failed"));

        assertEquals(100L, service.checkout(7L, dto));
    }

    @Test
    void checkout_missingSource_throwsParameterInvalid() {
        BizException exception = assertThrows(BizException.class,
            () -> service.checkout(7L, new OrderCheckoutDTO()));

        assertEquals(CommonCode.PARAM_INVALID, exception.getCode());
    }

    private OrderCheckoutDTO cartCheckoutDto(List<Long> ids) {
        OrderCheckoutDTO dto = new OrderCheckoutDTO();
        dto.setCheckoutSource(CheckoutSourceEnum.CART);
        dto.setCartItemIds(ids);
        return dto;
    }

    private OrderCheckoutDTO buyNowDto() {
        OrderCheckoutDTO dto = new OrderCheckoutDTO();
        dto.setCheckoutSource(CheckoutSourceEnum.BUY_NOW);
        dto.setItems(List.of(new OrderCheckoutItemDTO(100L, 1)));
        return dto;
    }
}
