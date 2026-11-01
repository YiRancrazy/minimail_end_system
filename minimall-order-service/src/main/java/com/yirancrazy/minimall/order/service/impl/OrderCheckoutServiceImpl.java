package com.yirancrazy.minimall.order.service.impl;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import com.yirancrazy.minimall.order.service.OrderCheckoutService;
import com.yirancrazy.minimall.order.service.OrderService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现结算来源分流、购物车快照校验和订单创建后的立即清理。
 *   业务边界：购物车来源要求条目去重且不超过 100 条，立即购买要求 items 非空；清理失败不回滚已创建订单。
 *   使用场景：用户结算接口通过该服务协调 OrderService 与 CartCleanupService。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    private static final int MAX_CART_ITEM_COUNT = 100;

    private final OrderService orderService;
    private final CartCleanupService cartCleanupService;
    private final OrderRemoteGateway orderRemoteGateway;

    /**
     * 提交用户结算请求。
     *
     * <p>用户 ID 取自请求头，结算来源和明细由 OrderCheckoutService 校验并创建订单。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 用户结算请求
     * @return 新订单 ID 的统一响应。
     * @throws BizException 结算参数、快照、库存或订单创建失败。
     */
    @Override
    public Long checkout(Long userId, OrderCheckoutDto dto) {
        CheckoutPreparation preparation = prepare(userId, dto);
        CheckoutResultDto result =
                orderService.checkout(
                        userId, preparation.orderItems, preparation.cleanupItems, dto.getReceiver());
        if (result == null || result.getOrderId() == null) {
            throw new BizException(CommonCode.SYS_ERROR, "订单创建失败");
        }
        if (result.getCartCleanupTaskId() != null) {
            try {
                cartCleanupService.tryCleanup(result.getCartCleanupTaskId());
            } catch (RuntimeException exception) {
                log.error("购物车立即清理异常，等待定时重试，taskId={}", result.getCartCleanupTaskId(), exception);
            }
        }
        return result.getOrderId();
    }

    /**
     * 根据结算来源生成订单明细和清理明细。
     *
     * <p>CART 读取并校验服务端购物车快照；BUY_NOW 直接使用请求明细且禁止携带购物车项 ID。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 用户结算请求
     * @return 结算准备对象。
     * @throws BizException 来源缺失或参数组合非法。
     */
    private CheckoutPreparation prepare(Long userId, OrderCheckoutDto dto) {
        if (dto == null || dto.getCheckoutSource() == null) {
            throw new BizException(CommonCode.PARAM_INVALID, "结算来源不能为空");
        }
        if (dto.getCheckoutSource() == CheckoutSourceEnum.CART) {
            return prepareCartCheckout(userId, dto);
        }
        if (dto.getItems() == null
                || dto.getItems().isEmpty()
                || dto.getCartItemIds() != null && !dto.getCartItemIds().isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "立即购买参数错误");
        }
        return new CheckoutPreparation(dto.getItems(), List.of());
    }

    /**
     * 加载购物车结算的服务端快照。
     *
     * <p>请求 ID 必须非空、去重且不超过 100 条，返回集合必须与请求 ID 完全一致。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 用户结算请求
     * @return 订单明细与购物车清理明细。
     * @throws BizException 参数非法、远端失败或快照缺失。
     */
    private CheckoutPreparation prepareCartCheckout(Long userId, OrderCheckoutDto dto) {
        if (dto.getCartItemIds() == null
                || dto.getCartItemIds().isEmpty()
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
        Result<List<CartCheckoutItemDto>> snapshot =
                orderRemoteGateway.cartCheckoutSnapshot(new CartCheckoutQueryDto(userId, ids));
        if (snapshot == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        if (!CommonCode.SUCCESS.equals(snapshot.getCode())) {
            throw new BizException(snapshot.getCode(), snapshot.getMessage());
        }
        if (snapshot.getData() == null || snapshot.getData().size() != ids.size()) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        Set<Long> actualIds =
                snapshot.getData().stream()
                        .map(CartCheckoutItemDto::getCartItemId)
                        .collect(Collectors.toSet());
        if (!actualIds.equals(distinctIds)) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        List<OrderCheckoutItemDto> orderItems =
                snapshot.getData().stream()
                        .map(item -> new OrderCheckoutItemDto(item.getSkuId(), item.getQuantity()))
                        .toList();
        return new CheckoutPreparation(orderItems, snapshot.getData());
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 保存一次结算准备阶段的订单明细和购物车清理明细。
     *   业务边界：该不可变内部载体只服务于 OrderCheckoutServiceImpl 的分支返回，不进入持久化或跨服务协议。
     *   使用场景：由 prepare 与 prepareCartCheckout 构造，随后传入 OrderService.checkout。
     * @Version: 1.0
     * @DateTime: 2026/10/23
     */
    private static final class CheckoutPreparation {

        private final List<OrderCheckoutItemDto> orderItems;
        private final List<CartCheckoutItemDto> cleanupItems;

        /**
         * 创建结算准备结果。
         *
         * <p>保存已解析的订单明细和所需清理的购物车明细，对象由结算编排内部使用。
         *
         * @param orderItems 订单明细列表
         * @param cleanupItems 结算成功后待清理的购物车条目快照
         */
        private CheckoutPreparation(
                List<OrderCheckoutItemDto> orderItems, List<CartCheckoutItemDto> cleanupItems) {
            this.orderItems = orderItems;
            this.cleanupItems = cleanupItems;
        }
    }
}
