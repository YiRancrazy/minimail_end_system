package com.yirancrazy.minimall.order.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDTO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDTO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderCheckoutService;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算编排服务实现。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    private static final int MAX_CART_ITEM_COUNT = 100;

    private final OrderService orderService;
    private final CartCleanupService cartCleanupService;
    private final OrderRemoteGateway orderRemoteGateway;

    /**
     * 创建结算订单并触发购物车清理。
     * @param userId 用户 ID
     * @param dto 结算参数
     * @return 订单 ID
     */
    @Override
    public Long checkout(Long userId, OrderCheckoutDTO dto) {
        CheckoutPreparation preparation = prepare(userId, dto);
        CheckoutResultDTO result = orderService.checkout(
            userId, preparation.orderItems, preparation.cleanupItems, dto.getReceiver());
        if (result == null || result.getOrderId() == null) {
            throw new BizException(CommonCode.SYS_ERROR, "订单创建失败");
        }
        if (result.getCartCleanupTaskId() != null) {
            try {
                cartCleanupService.tryCleanup(result.getCartCleanupTaskId());
            }
            catch (RuntimeException exception) {
                log.error("购物车立即清理异常，等待定时重试，taskId={}",
                    result.getCartCleanupTaskId(), exception);
            }
        }
        return result.getOrderId();
    }

    private CheckoutPreparation prepare(Long userId, OrderCheckoutDTO dto) {
        if (dto == null || dto.getCheckoutSource() == null) {
            throw new BizException(CommonCode.PARAM_INVALID, "结算来源不能为空");
        }
        if (dto.getCheckoutSource() == CheckoutSourceEnum.CART) {
            return prepareCartCheckout(userId, dto);
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()
            || dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "立即购买参数错误");
        }
        return new CheckoutPreparation(dto.getItems(), List.of());
    }

    private CheckoutPreparation prepareCartCheckout(Long userId, OrderCheckoutDTO dto) {
        if (dto.getCartItemIds() == null || dto.getCartItemIds().isEmpty()
            || dto.getItems() != null && !dto.getItems().isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "购物车结算参数错误");
        }
        List<Long> ids = dto.getCartItemIds();
        Set<Long> distinctIds = new HashSet<>(ids);
        if (distinctIds.size() != ids.size()) {
            throw new BizException(CommonCode.PARAM_INVALID, "购物车条目重复");
        }
        if (ids.size() > MAX_CART_ITEM_COUNT) {
            throw new BizException(CommonCode.PARAM_INVALID, "购物车条目超过上限");
        }
        Result<List<CartCheckoutItemDTO>> snapshot = orderRemoteGateway
            .cartCheckoutSnapshot(new CartCheckoutQueryDTO(userId, ids));
        if (snapshot == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        if (!CommonCode.SUCCESS.equals(snapshot.getCode())) {
            throw new BizException(snapshot.getCode(), snapshot.getMessage());
        }
        if (snapshot.getData() == null || snapshot.getData().size() != ids.size()) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        Set<Long> actualIds = snapshot.getData().stream()
            .map(CartCheckoutItemDTO::getCartItemId)
            .collect(Collectors.toSet());
        if (!actualIds.equals(distinctIds)) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        List<OrderCheckoutItemDTO> orderItems = snapshot.getData().stream()
            .map(item -> new OrderCheckoutItemDTO(item.getSkuId(), item.getQuantity()))
            .toList();
        return new CheckoutPreparation(orderItems, snapshot.getData());
    }

    private static final class CheckoutPreparation {

        private final List<OrderCheckoutItemDTO> orderItems;
        private final List<CartCheckoutItemDTO> cleanupItems;

        private CheckoutPreparation(
                List<OrderCheckoutItemDTO> orderItems,
                List<CartCheckoutItemDTO> cleanupItems) {
            this.orderItems = orderItems;
            this.cleanupItems = cleanupItems;
        }
    }
}
