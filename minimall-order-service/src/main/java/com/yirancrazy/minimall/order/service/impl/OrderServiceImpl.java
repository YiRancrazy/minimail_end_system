package com.yirancrazy.minimall.order.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务实现，编排下单核心链路：取商品快照→锁库存→创建支付流水→持久化订单，并通过事件总线广播订单状态变更。
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int PAY_EXPIRE_MINUTES = 30;
    private static final int AUTO_CONFIRM_DAYS = 15;
    private static final int ORDER_TYPE_NORMAL = 1;
    private static final int REFUND_STATUS_NONE = 0;

    private final OrderManager orderManager;
    private final OrderItemManager orderItemManager;
    private final OrderLogisticsManager orderLogisticsManager;
    private final OrderStatusLogManager orderStatusLogManager;
    private final OrderMapper orderMapper;
    private final GoodsFeignClient goodsFeignClient;
    private final StockFeignClient stockFeignClient;
    private final PayFeignClient payFeignClient;
    private final EventBus eventBus;
    private final OrderStatusMachine statusMachine;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(OrderManager orderManager,
                            OrderItemManager orderItemManager,
                            OrderLogisticsManager orderLogisticsManager,
                            OrderStatusLogManager orderStatusLogManager,
                            OrderMapper orderMapper,
                            GoodsFeignClient goodsFeignClient,
                            StockFeignClient stockFeignClient,
                            PayFeignClient payFeignClient,
                            EventBus eventBus,
                            OrderStatusMachine statusMachine,
                            ObjectMapper objectMapper) {
        this.orderManager = orderManager;
        this.orderItemManager = orderItemManager;
        this.orderLogisticsManager = orderLogisticsManager;
        this.orderStatusLogManager = orderStatusLogManager;
        this.orderMapper = orderMapper;
        this.goodsFeignClient = goodsFeignClient;
        this.stockFeignClient = stockFeignClient;
        this.payFeignClient = payFeignClient;
        this.eventBus = eventBus;
        this.statusMachine = statusMachine;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建订单：取商品快照算金额，Seata AT 全局事务内锁库存与创建支付流水，落库后返回订单ID。
     * @param userId 用户ID
     * @param skuId SKU ID
     * @param quantity 购买数量
     * @return 订单ID
     */
    @Override
    @GlobalTransactional
    public Long create(Long userId, Long skuId, Integer quantity) {
        SkuSnapshotDTO snapshot = goodsFeignClient.skuSnapshot(skuId).getData();
        if (snapshot == null || snapshot.getPrice() == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        BigDecimal amount = snapshot.getPrice().multiply(BigDecimal.valueOf(quantity));

        Boolean reserved = stockFeignClient.reserve(new StockReserveDTO(skuId, quantity)).getData();
        if (reserved == null || !reserved) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }

        OrderPO po = new OrderPO();
        po.setOrderNo(generateOrderNo());
        po.setOrderType(ORDER_TYPE_NORMAL);
        po.setUserId(userId);
        po.setSkuId(skuId);
        po.setQuantity(quantity);
        po.setAmount(amount);
        po.setTotalAmount(amount);
        po.setPayAmount(amount);
        po.setFreightAmount(BigDecimal.ZERO);
        po.setDiscountAmount(BigDecimal.ZERO);
        po.setPayExpireAt(LocalDateTime.now().plusMinutes(PAY_EXPIRE_MINUTES));
        po.setStatus(OrderStatusEnum.PENDING.intCode());
        orderManager.save(po);

        Long payId = payFeignClient.create(
            new PayCreateDTO(String.valueOf(po.getId()), userId, 0L, amount, null)).getData();
        if (payId == null || payId < 0) {
            throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
        }
        po.setPayId(payId);
        orderManager.updateById(po);

        log.info("order created, orderId={}, userId={}, amount={}, payId={}",
            po.getId(), userId, amount, payId);
        return po.getId();
    }

    /**
     * 多SKU结算下单：按商家分组拆单，每组创建独立 t_order 共享同一 orderGroupNo，并各自初始化支付流水。
     * ponytail: 单商家时退化为单订单，行为与旧版本兼容。
     * @param userId 用户ID
     * @param items 结算明细列表
     * @return 主订单ID（首个创建的子订单）
     */
    @Override
    @GlobalTransactional
    public Long checkout(Long userId, List<OrderCheckoutItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(OrderCodeEnum.ORDER_ITEMS_EMPTY);
        }

        String orderGroupNo = "OG" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        java.util.Map<Long, List<OrderItemPO>> itemsByMerchant = new java.util.LinkedHashMap<>();

        for (OrderCheckoutItemDTO item : items) {
            SkuSnapshotDTO snapshot = goodsFeignClient.skuSnapshot(item.getSkuId()).getData();
            if (snapshot == null || snapshot.getPrice() == null) {
                throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
            }
            if (snapshot.getMerchantId() == null) {
                throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
            }
            Boolean reserved = stockFeignClient.reserve(
                new StockReserveDTO(item.getSkuId(), item.getQuantity())).getData();
            if (reserved == null || !reserved) {
                throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
            }

            BigDecimal lineAmount = snapshot.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

            OrderItemPO itemPO = new OrderItemPO();
            itemPO.setSkuId(item.getSkuId());
            itemPO.setSpuId(snapshot.getSpuId());
            itemPO.setMerchantId(snapshot.getMerchantId());
            itemPO.setSkuName(snapshot.getSkuName());
            itemPO.setQuantity(item.getQuantity());
            itemPO.setUnitPrice(snapshot.getPrice());
            itemPO.setAmount(lineAmount);
            itemPO.setSubtotalAmount(lineAmount);
            itemPO.setDiscountAmount(BigDecimal.ZERO);
            itemPO.setPayAmount(lineAmount);
            itemPO.setRefundStatus(REFUND_STATUS_NONE);
            itemPO.setSkuSnapshotJson(toSnapshotJson(snapshot));
            itemPO.setSpuSnapshotJson(toSnapshotJson(java.util.Map.of("spuId", snapshot.getSpuId())));

            Long merchantId = snapshot.getMerchantId();
            itemsByMerchant.computeIfAbsent(merchantId, k -> new ArrayList<>()).add(itemPO);
        }

        Long mainOrderId = null;
        for (java.util.Map.Entry<Long, List<OrderItemPO>> entry : itemsByMerchant.entrySet()) {
            Long merchantId = entry.getKey();
            List<OrderItemPO> merchantItems = entry.getValue();
            BigDecimal groupAmount = calcAmount(merchantItems);

            OrderPO po = new OrderPO();
            po.setOrderNo(generateOrderNo());
            po.setOrderType(ORDER_TYPE_NORMAL);
            po.setUserId(userId);
            po.setMerchantId(merchantId);
            po.setAmount(groupAmount);
            po.setTotalAmount(groupAmount);
            po.setPayAmount(groupAmount);
            po.setFreightAmount(BigDecimal.ZERO);
            po.setDiscountAmount(BigDecimal.ZERO);
            po.setPayExpireAt(LocalDateTime.now().plusMinutes(PAY_EXPIRE_MINUTES));
            po.setStatus(OrderStatusEnum.PENDING.intCode());
            po.setOrderGroupNo(orderGroupNo);
            orderManager.save(po);

            for (OrderItemPO itemPO : merchantItems) {
                itemPO.setOrderId(po.getId());
                orderItemManager.save(itemPO);
            }

            Long payId = payFeignClient.create(
                new PayCreateDTO(String.valueOf(po.getId()), userId, merchantId, groupAmount, null)).getData();
            if (payId == null || payId < 0) {
                throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
            }
            po.setPayId(payId);
            orderManager.updateById(po);

            if (mainOrderId == null) {
                mainOrderId = po.getId();
            }
            log.info("order checkout split, orderId={}, userId={}, merchantId={}, groupNo={}, items={}, amount={}",
                po.getId(), userId, merchantId, orderGroupNo, merchantItems.size(), groupAmount);
        }

        log.info("order checkout done, userId={}, groupNo={}, subOrders={}",
            userId, orderGroupNo, itemsByMerchant.size());
        return mainOrderId;
    }

    @Override
    public void pay(Long orderId) {
        OrderPO po = getOrder(orderId);
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, po.getUserId(), po.getAmount(), LocalDateTime.now().toString());
        eventBus.publishInTx(event,
            () -> transitStatus(orderId, OrderStatusEnum.PAID, "USER_PAY", null),
            e -> isOrderPaid(orderId));
        log.info("order paid, orderId={}", orderId);
    }

    /**
     * 按业务单号推进订单为已支付，C 端支付回调携带业务单号而非订单ID，不存在时抛出 ORDER_NOT_FOUND。
     * @param orderNo 业务单号
     */
    @Override
    public void payByOrderNo(String orderNo) {
        OrderPO po = orderManager.getOne(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getOrderNo, orderNo)
            .last("LIMIT 1"));
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        pay(po.getId());
    }

    /**
     * Check whether the order has reached PAID status, used as the
     * transaction-message callback checker.
     * @param orderId the order id to probe
     * @return true if the order is PAID
     */
    private boolean isOrderPaid(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        return po != null && po.getStatus() == OrderStatusEnum.PAID.intCode();
    }

    @Override
    public void cancel(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.CANCELED, "USER_CANCEL", "USER_CANCEL");
        releaseStockForOrder(po);
        log.info("order cancelled, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void ship(Long orderId, Long merchantId, String carrier, String trackingNo) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.SHIPPED, "MERCHANT_SHIP", null);
        OrderLogisticsPO node = new OrderLogisticsPO();
        node.setOrderId(orderId);
        node.setNode("已发货");
        node.setDescription(String.format("%s · %s", carrier, trackingNo));
        node.setCreatedTime(LocalDateTime.now());
        orderLogisticsManager.save(node);
        log.info("order shipped, orderId={}, merchantId={}, carrier={}, trackingNo={}",
            orderId, merchantId, carrier, trackingNo);
    }

    @Override
    public void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        OrderStatusEnum current = statusMachine.fromCode(po.getStatus());
        if (current != OrderStatusEnum.PAID && current != OrderStatusEnum.SHIPPED
                && current != OrderStatusEnum.COMPLETED) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        // 商家主动发起退款与用户申请走相同状态机：PAID/SHIPPED/COMPLETED → REFUNDING，由商家审核后进入 REFUNDED
        if (current == OrderStatusEnum.COMPLETED) {
            // 已完成的订单需要回退到 SHIPPED 状态机入口走相同路径
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        refund(orderId, po.getUserId());
        log.info("merchant initiated refund, orderId={}, merchantId={}, amount={}, reason={}",
            orderId, merchantId, refundAmount, reason);
    }

    @Override
    public void confirm(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.COMPLETED, "USER_CONFIRM", null);
        log.info("order confirmed, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void refund(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        OrderStatusEnum current = statusMachine.fromCode(po.getStatus());
        if (!statusMachine.canTransitTo(current, OrderStatusEnum.REFUNDING)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        // 申请退款仅登记退款意向（记录原状态并置 REFUNDING），真实退款由商家审核通过后调用支付网关触发
        po.setRefundFromStatus(po.getStatus());
        transition(po, OrderStatusEnum.REFUNDING, "USER_APPLY_REFUND", null);
        log.info("order refunding, orderId={}", orderId);
    }

    @Override
    public void handleRefundCallback(Long orderId, boolean success) {
        OrderPO po = getOrder(orderId);
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            // 商家已驳回或先行回调已推进状态：拒绝迟到回调，天然幂等
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        if (success) {
            transitStatus(orderId, OrderStatusEnum.REFUNDED, "PAY_REFUND_CALLBACK", null);
            log.info("order refunded, orderId={}", orderId);
        }
        else {
            transitStatus(orderId, revertTarget(po), "PAY_REFUND_CALLBACK_FAIL", null);
            log.info("order refund failed, reverted, orderId={}", orderId);
        }
    }

    @Override
    public Integer getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
    }

    /**
     * 按订单标识解析归属商户：纯数字按订单ID查，其余按业务单号查，不存在返回 null。
     * 支付服务在 C 端无商户上下文的场景用此接口归属收款商户。
     * @param ref 订单标识
     * @return 商户ID，订单不存在时返回 null
     */
    @Override
    public Long resolveMerchantId(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        OrderPO po = null;
        if (ref.matches("\\d+")) {
            try {
                po = orderManager.getById(Long.valueOf(ref));
            }
            catch (NumberFormatException e) {
                po = null;
            }
        }
        if (po == null) {
            po = orderManager.getOne(Wrappers.lambdaQuery(OrderPO.class)
                .eq(OrderPO::getOrderNo, ref)
                .last("LIMIT 1"));
        }
        return po == null ? null : po.getMerchantId();
    }

    /**
     * 查询订单详情，不存在时抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @return 订单持久化实体
     */
    @Override
    public OrderPO getDetail(Long orderId) {
        return getOrder(orderId);
    }

    /**
     * 商家关闭订单，仅允许 PENDING 状态关闭并释放库存，归属不符时抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     */
    @Override
    public void merchantClose(Long orderId, Long merchantId) {
        OrderPO po = getOrder(orderId);
        if (!po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.CANCELED, "MERCHANT_CLOSE", "MERCHANT_CLOSE");
        releaseStockForOrder(po);
        log.info("order merchant-closed, orderId={}, merchantId={}", orderId, merchantId);
    }

    /**
     * 用户删除订单，仅允许终态（CANCELED/COMPLETED/REFUNDED）删除，归属不符抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    @Override
    public void delete(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        Integer status = po.getStatus();
        if (!status.equals(OrderStatusEnum.CANCELED.intCode())
            && !status.equals(OrderStatusEnum.COMPLETED.intCode())
            && !status.equals(OrderStatusEnum.REFUNDED.intCode())) {
            throw new BizException(OrderCodeEnum.ORDER_DELETE_NOT_ALLOWED);
        }
        orderManager.removeById(orderId);
        log.info("order deleted, orderId={}, userId={}", orderId, userId);
    }

    /**
     * 平台强制关闭异常订单，允许 PENDING 或 REFUNDING 状态关闭并释放库存。
     * @param orderId 订单ID
     */
    @Override
    public void platformClose(Long orderId) {
        OrderPO po = getOrder(orderId);
        transitStatus(orderId, OrderStatusEnum.CANCELED, "PLATFORM_CLOSE", "PLATFORM_CLOSE");
        releaseStockForOrder(po);
        log.info("order platform-closed, orderId={}", orderId);
    }

    /**
     * 商家待处理订单数量统计，包含 PENDING/PAID/REFUNDING 三种状态。
     * @param merchantId 商家ID
     * @return 待处理订单总数
     */
    @Override
    public long pendingCount(Long merchantId) {
        return orderManager.count(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getMerchantId, merchantId)
            .in(OrderPO::getStatus, List.of(
                OrderStatusEnum.PENDING.intCode(),
                OrderStatusEnum.PAID.intCode(),
                OrderStatusEnum.REFUNDING.intCode())));
    }

    private OrderPO getOrder(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }

    /**
     * 释放订单库存：优先查询 order_item 明细逐条释放，无明细时回退到订单头的 skuId/quantity（兼容旧单SKU订单）。
     * @param po 订单实体
     */
    private void releaseStockForOrder(OrderPO po) {
        List<OrderItemPO> items = orderItemManager.list(
            Wrappers.lambdaQuery(OrderItemPO.class).eq(OrderItemPO::getOrderId, po.getId()));
        if (!items.isEmpty()) {
            for (OrderItemPO item : items) {
                Boolean released = stockFeignClient.release(
                    new StockReserveDTO(item.getSkuId(), item.getQuantity())).getData();
                if (released == null || !released) {
                    log.warn("stock release failed, orderId={}, skuId={}", po.getId(), item.getSkuId());
                }
            }
            return;
        }
        if (po.getSkuId() != null && po.getQuantity() != null) {
            Boolean released = stockFeignClient.release(
                new StockReserveDTO(po.getSkuId(), po.getQuantity())).getData();
            if (released == null || !released) {
                log.warn("stock release failed, orderId={}, skuId={}", po.getId(), po.getSkuId());
            }
        }
    }

    /**
     * 推进订单状态并落状态日志：写入目标状态对应的时间戳，CANCELED 额外记录关闭原因。
     * @param orderId 订单ID
     * @param target 目标状态
     * @param triggerSource 触发来源（状态日志）
     * @param closeReason 关闭原因（仅 CANCELED 使用）
     */
    private void transitStatus(Long orderId, OrderStatusEnum target, String triggerSource, String closeReason) {
        transition(getOrder(orderId), target, triggerSource, closeReason);
    }

    /**
     * 推进订单状态：状态机校验→写目标时间戳→更新影响 0 行即抛状态流转异常（并发防抖），
     * 全部成功后才落状态日志，保证副作用只发生在状态真正迁移之后。
     * @param po 待推进的订单实体（已校验归属）
     * @param target 目标状态
     * @param triggerSource 触发来源（状态日志）
     * @param closeReason 关闭原因（仅 CANCELED 使用）
     */
    private void transition(OrderPO po, OrderStatusEnum target, String triggerSource, String closeReason) {
        Integer fromStatus = po.getStatus();
        statusMachine.transit(po, target);
        applyTargetTimestamps(po, target, closeReason);
        if (!orderManager.updateById(po)) {
            // 并发下后写者更新 0 行：目标状态已被先行事务推进，拒绝继续执行副作用
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        saveStatusLog(po, fromStatus, target, triggerSource);
    }

    /**
     * 解析退款回退目标状态：优先使用申请退款时记录的 refundFromStatus，缺失时兜底 PAID。
     * @param po 退款中订单实体
     * @return 回退目标状态
     */
    private OrderStatusEnum revertTarget(OrderPO po) {
        Integer fromStatus = po.getRefundFromStatus();
        return fromStatus != null ? statusMachine.fromCode(fromStatus) : OrderStatusEnum.PAID;
    }

    /**
     * 按目标状态写入对应业务时间戳。
     * @param po 订单实体
     * @param target 目标状态
     * @param closeReason 关闭原因
     */
    private void applyTargetTimestamps(OrderPO po, OrderStatusEnum target, String closeReason) {
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case PAID -> po.setPaidAt(now);
            case SHIPPED -> po.setShippedAt(now);
            case COMPLETED -> po.setReceivedAt(now);
            case CANCELED -> {
                po.setClosedAt(now);
                po.setCloseReason(closeReason);
            }
            case REFUNDED -> po.setClosedAt(now);
            default -> {
            }
        }
    }

    /**
     * 落订单状态机日志，供事件溯源与审计追溯。
     * @param po 订单实体
     * @param fromStatus 原状态码
     * @param toStatus 目标状态
     * @param triggerSource 触发来源
     */
    private void saveStatusLog(OrderPO po, Integer fromStatus, OrderStatusEnum toStatus, String triggerSource) {
        OrderStatusLogPO log = new OrderStatusLogPO();
        log.setOrderId(po.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus.intCode());
        log.setTriggerSource(triggerSource);
        orderStatusLogManager.save(log);
    }

    /**
     * 生成业务单号：OD + 时间戳（毫秒级） + 3 位随机，保证同一毫秒内不冲突。
     * @return 业务单号
     */
    private String generateOrderNo() {
        return "OD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
            + (int) (Math.random() * 1000);
    }

    /**
     * 序列化订单快照 JSON，失败时返回 null（不阻断下单）。
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    private String toSnapshotJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
            log.warn("serialize order snapshot failed", e);
            return null;
        }
    }

    /**
     * 计算订单总金额，等于各订单行金额之和。精度 scale=2，RoundingMode.HALF_EVEN。
     * @param itemPOs 订单行列表
     * @return 订单总金额
     */
    private BigDecimal calcAmount(List<OrderItemPO> itemPOs) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemPO item : itemPOs) {
            total = total.add(item.getAmount());
        }
        return total.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    /**
     * 游标分页查询订单，按 userId/merchantId/status 等值过滤，按ID倒序。
     * @param dto 游标分页查询入参
     * @return 订单游标分页结果
     */
    @Override
    public CursorPageVO<OrderPO> page(OrderPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        Integer statusCode = resolveStatusCode(dto.getStatus());
        List<OrderPO> records = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .lt(lastId != null, OrderPO::getId, lastId)
            .eq(dto.getUserId() != null, OrderPO::getUserId, dto.getUserId())
            .eq(dto.getMerchantId() != null, OrderPO::getMerchantId, dto.getMerchantId())
            .eq(statusCode != null, OrderPO::getStatus, statusCode)
            .ge(dto.getStartTime() != null, OrderPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, OrderPO::getCreateTime, dto.getEndTime())
            .orderByDesc(OrderPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, OrderPO::getId);
    }

    /**
     * 导出商家订单列表，最多 10000 行，merchantId 强制绑定。
     * @param merchantId 商家ID
     * @param dto 查询入参
     * @return 订单列表
     */
    @Override
    public List<OrderPO> exportList(Long merchantId, OrderPageDTO dto) {
        Integer statusCode = resolveStatusCode(dto.getStatus());
        return orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getMerchantId, merchantId)
            .eq(dto.getUserId() != null, OrderPO::getUserId, dto.getUserId())
            .eq(statusCode != null, OrderPO::getStatus, statusCode)
            .ge(dto.getStartTime() != null, OrderPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, OrderPO::getCreateTime, dto.getEndTime())
            .orderByDesc(OrderPO::getCreateTime)
            .last("LIMIT " + CsvExporter.maxExportRows()));
    }

    /**
     * 导出全平台订单列表，最多 10000 行，不绑定 merchantId。
     * @param dto 查询入参
     * @return 订单列表
     */
    @Override
    public List<OrderPO> platformExportList(OrderPageDTO dto) {
        Integer statusCode = resolveStatusCode(dto.getStatus());
        return orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .eq(dto.getUserId() != null, OrderPO::getUserId, dto.getUserId())
            .eq(dto.getMerchantId() != null, OrderPO::getMerchantId, dto.getMerchantId())
            .eq(statusCode != null, OrderPO::getStatus, statusCode)
            .ge(dto.getStartTime() != null, OrderPO::getCreateTime, dto.getStartTime())
            .le(dto.getEndTime() != null, OrderPO::getCreateTime, dto.getEndTime())
            .orderByDesc(OrderPO::getCreateTime)
            .last("LIMIT " + CsvExporter.maxExportRows()));
    }

    /**
     * 将订单状态枚举别名（如 PENDING）解析为持久化状态码；非法或空值返回 null 表示不过滤。
     * @param status 状态枚举别名
     * @return 持久化状态码或 null
     */
    private Integer resolveStatusCode(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getAlias().equalsIgnoreCase(status)) {
                return e.intCode();
            }
        }
        return null;
    }

    /**
     * 平台订单统计聚合，可选 merchantId 过滤，委托 OrderMapper 统计。
     * @param dto 查询入参（复用 merchantId/startTime/endTime）
     * @return 订单统计VO
     */
    @Override
    public OrderStatisticsVO statistics(OrderPageDTO dto) {
        return orderMapper.statistics(dto.getMerchantId(), dto.getStartTime(), dto.getEndTime());
    }

    /**
     * 统计指定用户各状态订单数量：委托 OrderMapper 单条 GROUP BY 查询。
     * @param userId 用户ID
     * @return 各状态订单数量VO
     */
    @Override
    public OrderStatusCountsVO countStatusByUser(Long userId) {
        List<java.util.Map<String, Object>> rows = orderMapper.countByStatus(userId);
        OrderStatusCountsVO vo = new OrderStatusCountsVO();
        for (java.util.Map<String, Object> row : rows) {
            Number status = (Number) row.get("status");
            Number cnt = (Number) row.get("cnt");
            if (status == null || cnt == null) {
                continue;
            }
            long count = cnt.longValue();
            switch (status.intValue()) {
                case 1 -> vo.setPendingCount(count);
                case 2 -> vo.setPaidCount(count);
                case 3 -> vo.setShippedCount(count);
                case 4 -> vo.setCompletedCount(count);
                default -> {
                }
            }
        }
        return vo;
    }

    /**
     * 商家审核退款，校验订单归属与 REFUNDING 状态；approved=true 才发起真实退款（状态由支付回调驱动到 REFUNDED），
     * false 回退到 refundFromStatus。
     * @param orderId 订单ID
     * @param approved 是否同意退款
     * @param merchantId 商家ID
     */
    @Override
    public void reviewRefund(Long orderId, boolean approved, Long merchantId) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_REFUNDING);
        }
        if (!approved) {
            transition(po, revertTarget(po), "MERCHANT_REJECT_REFUND", null);
            log.info("refund rejected by merchant, orderId={}, merchantId={}", orderId, merchantId);
            return;
        }
        // 审核通过才调支付网关真实退款，避免"先退钱后审核"；状态迁移由支付回调 handleRefundCallback 驱动
        Boolean refunded = payFeignClient.refund(
            new RefundCreateDTO(po.getPayId(), po.getAmount(), null)).getData();
        if (refunded == null || !refunded) {
            log.warn("pay refund failed, orderId={}", orderId);
        }
        log.info("refund approved by merchant, orderId={}, merchantId={}", orderId, merchantId);
    }

    /**
     * 平台退款仲裁，校验 REFUNDING 状态；approved=true 推进 REFUNDED，false 回退到 refundFromStatus。
     * @param orderId 订单ID
     * @param approved 仲裁是否支持退款
     */
    @Override
    public void arbitrateRefund(Long orderId, boolean approved) {
        OrderPO po = getOrder(orderId);
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_REFUNDING);
        }
        if (approved) {
            transitStatus(orderId, OrderStatusEnum.REFUNDED, "PLATFORM_ARBITRATE", null);
            log.info("refund arbitrated approved, orderId={}", orderId);
            return;
        }
        transitStatus(orderId, revertTarget(po), "PLATFORM_ARBITRATE_REJECT", null);
        log.info("refund arbitrated rejected, orderId={}", orderId);
    }

    /**
     * 查询订单物流轨迹，按创建时间正序返回；订单不存在抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @return 物流节点列表
     */
    @Override
    public List<OrderLogisticsVO> queryLogistics(Long orderId) {
        getOrder(orderId);
        List<OrderLogisticsPO> nodes = orderLogisticsManager.list(
            Wrappers.lambdaQuery(OrderLogisticsPO.class)
                .eq(OrderLogisticsPO::getOrderId, orderId)
                .orderByAsc(OrderLogisticsPO::getCreateTime));
        return nodes.stream().map(this::toLogisticsVO).collect(Collectors.toList());
    }

    private OrderLogisticsVO toLogisticsVO(OrderLogisticsPO po) {
        OrderLogisticsVO vo = new OrderLogisticsVO();
        vo.setNode(po.getNode());
        vo.setDescription(po.getDescription());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 扫描超时未支付订单（PENDING 且 pay_expire_at 已过期；旧数据无 pay_expire_at 时回退 create_time+30min），逐个取消并释放库存。
     * @return 处理的订单数
     */
    @Override
    public int scanExpiredOrders() {
        LocalDateTime expireThreshold = LocalDateTime.now().minusMinutes(PAY_EXPIRE_MINUTES);
        List<OrderPO> expired = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getStatus, OrderStatusEnum.PENDING.intCode())
            .and(w -> w.isNull(OrderPO::getPayExpireAt).lt(OrderPO::getCreateTime, expireThreshold)
                .or().lt(OrderPO::getPayExpireAt, expireThreshold)));
        int count = 0;
        for (OrderPO po : expired) {
            try {
                transitStatus(po.getId(), OrderStatusEnum.CANCELED, "SYSTEM_TIMEOUT", "SYSTEM_TIMEOUT");
                releaseStockForOrder(po);
                count++;
            }
            catch (Exception e) {
                log.warn("scanExpiredOrders cancel failed, orderId={}, err={}", po.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("scanExpiredOrders cancelled {} orders", count);
        }
        return count;
    }

    /**
     * 扫描发货后超期未确认收货订单（SHIPPED 且 shipped_at + 15d < NOW()），逐个推进 COMPLETED。
     * @return 处理的订单数
     */
    @Override
    public int scanAutoConfirm() {
        LocalDateTime confirmThreshold = LocalDateTime.now().minusDays(AUTO_CONFIRM_DAYS);
        List<OrderPO> overdue = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getStatus, OrderStatusEnum.SHIPPED.intCode())
            .lt(OrderPO::getShippedAt, confirmThreshold));
        int count = 0;
        for (OrderPO po : overdue) {
            try {
                transitStatus(po.getId(), OrderStatusEnum.COMPLETED, "SYSTEM_AUTO_CONFIRM", null);
                count++;
            }
            catch (Exception e) {
                log.warn("scanAutoConfirm confirm failed, orderId={}, err={}", po.getId(), e.getMessage());
            }
        }
        if (count > 0) {
            log.info("scanAutoConfirm completed {} orders", count);
        }
        return count;
    }
}
