package com.yirancrazy.minimall.order.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.OutboxStore;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.constant.StockReleaseStatusEnum;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDTO;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDTO;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDTO;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDTO;
import com.yirancrazy.minimall.order.dto.ReceiverDTO;
import com.yirancrazy.minimall.order.entity.OrderItemPO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVO;
import com.yirancrazy.minimall.order.vo.OrderItemVO;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;
import com.yirancrazy.minimall.order.vo.OrderSummaryVO;
import com.yirancrazy.minimall.order.vo.OrderTrendPointVO;
import com.yirancrazy.minimall.order.vo.OrderTrendVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int PAY_EXPIRE_MINUTES = 30;
    private static final int AUTO_CONFIRM_DAYS = 15;
    private static final int ORDER_TYPE_NORMAL = 1;
    private static final int REFUND_STATUS_NONE = 0;
    private static final int MAX_CHECKOUT_SKU_COUNT = 100;
    // 金额统一按“分”存储：两位小数，任何更高精度尾数一律拒绝而非静默进位
    private static final int AMOUNT_SCALE = 2;
    // 扫描分批参数：单批 200 条、单轮最多 50 批，避免一次性全表加载导致 OOM 与长事务
    private static final int SCAN_BATCH_SIZE = 200;
    private static final int SCAN_MAX_BATCHES = 50;
    // 可评价订单状态：仅已支付、已发货、已完成三种购买态允许评价
    private static final Set<Integer> PURCHASED_STATUS_CODES = Set.of(
        OrderStatusEnum.PAID.intCode(),
        OrderStatusEnum.SHIPPED.intCode(),
        OrderStatusEnum.COMPLETED.intCode());

    private final OrderManager orderManager;
    private final OrderItemManager orderItemManager;
    private final OrderLogisticsManager orderLogisticsManager;
    private final OrderStatusLogManager orderStatusLogManager;
    private final OrderRemoteGateway orderRemoteGateway;
    private final EventBus eventBus;
    private final OutboxStore outboxStore;
    private final TransactionTemplate transactionTemplate;
    private final OrderStatusMachine statusMachine;
    private final ObjectMapper objectMapper;
    private final MinioUtil minioUtil;

    /**
     * 构造 OrderServiceImpl 实例。
     * @param orderManager orderManager 参数
     * @param orderItemManager orderItemManager 参数
     * @param orderLogisticsManager orderLogisticsManager 参数
     * @param orderStatusLogManager orderStatusLogManager 参数
     * @param orderRemoteGateway orderRemoteGateway 参数
     * @param eventBus eventBus 参数
     * @param outboxStore outboxStore 参数
     * @param transactionTemplate transactionTemplate 参数
     * @param statusMachine statusMachine 参数
     * @param objectMapper objectMapper 参数
     * @param minioUtil minioUtil 参数
     */
    public OrderServiceImpl(OrderManager orderManager,
                            OrderItemManager orderItemManager,
                            OrderLogisticsManager orderLogisticsManager,
                            OrderStatusLogManager orderStatusLogManager,
                            OrderRemoteGateway orderRemoteGateway,
                            EventBus eventBus,
                            OutboxStore outboxStore,
                            TransactionTemplate transactionTemplate,
                            OrderStatusMachine statusMachine,
                            ObjectMapper objectMapper,
                            MinioUtil minioUtil) {
        this.orderManager = orderManager;
        this.orderItemManager = orderItemManager;
        this.orderLogisticsManager = orderLogisticsManager;
        this.orderStatusLogManager = orderStatusLogManager;
        this.orderRemoteGateway = orderRemoteGateway;
        this.eventBus = eventBus;
        this.outboxStore = outboxStore;
        this.transactionTemplate = transactionTemplate;
        this.statusMachine = statusMachine;
        this.objectMapper = objectMapper;
        this.minioUtil = minioUtil;
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    @Override
    public Long create(Long userId, Long skuId, Integer quantity) {
        return create(userId, skuId, quantity, null);
    }

    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param receiver receiver 参数
     * @return 业务数值
     */
    @Override
    @GlobalTransactional
    public Long create(Long userId, Long skuId, Integer quantity, ReceiverDTO receiver) {

        // 获取商品快照
        SkuSnapshotDTO snapshot = orderRemoteGateway.skuSnapshot(skuId).getData();
        if (snapshot == null || snapshot.getPrice() == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }

        // 统一小数位数
        BigDecimal amount = normalizeAmount(snapshot.getPrice().multiply(BigDecimal.valueOf(quantity)));

        // 锁库存
        Boolean reserved = orderRemoteGateway.reserve(new StockReserveDTO(skuId, quantity)).getData();
        if (reserved == null || !reserved) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }

        // 创建订单
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
        po.setReceiverSnapshotJson(toReceiverSnapshotJson(receiver));
        po.setPayExpireAt(LocalDateTime.now().plusMinutes(PAY_EXPIRE_MINUTES));
        po.setStatus(OrderStatusEnum.PENDING.intCode());
        orderManager.save(po);

        // 落订单明细快照：商品名不依赖 goods 实时/在售状态，商家列表与详情据此回显
        OrderItemPO itemPO = new OrderItemPO();
        itemPO.setOrderId(po.getId());
        itemPO.setSpuId(snapshot.getSpuId());
        itemPO.setMerchantId(snapshot.getMerchantId());
        itemPO.setSkuId(skuId);
        itemPO.setSkuName(snapshot.getSkuName());
        itemPO.setQuantity(quantity);
        itemPO.setUnitPrice(snapshot.getPrice());
        itemPO.setAmount(amount);
        itemPO.setSubtotalAmount(amount);
        itemPO.setDiscountAmount(BigDecimal.ZERO);
        itemPO.setPayAmount(amount);
        itemPO.setRefundStatus(REFUND_STATUS_NONE);
        orderItemManager.save(itemPO);

        // 创建支付流水：支付服务在 C 端无商户上下文，按订单归属商户收款
        Long payId = orderRemoteGateway.createPayment(
            new PayCreateDTO(String.valueOf(po.getId()), userId, 0L, amount, null)).getData();
        if (payId == null || payId < 0) {
            throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
        }
        po.setPayId(payId);
        orderManager.updateById(po);

        log.info("订单已创建，orderId={}，userId={}，amount={}，payId={}",
            po.getId(), userId, amount, payId);
        return po.getId();
    }


    /**
     * 校验业务条件。
     * @param userId userId 参数
     * @param items items 参数
     * @return 业务数值
     */
    @Override
    @GlobalTransactional
    public Long checkout(Long userId, List<OrderCheckoutItemDTO> items) {
        return checkout(userId, items, null);
    }

    /**
     * 校验业务条件。
     * @param userId userId 参数
     * @param items items 参数
     * @param receiver receiver 参数
     * @return 业务数值
     */
    @Override
    @GlobalTransactional
    public Long checkout(Long userId, List<OrderCheckoutItemDTO> items, ReceiverDTO receiver) {
        Map<Long, Integer> quantityBySku = mergeCheckoutItems(items);
        List<Long> skuIds = new ArrayList<>(quantityBySku.keySet());
        Map<Long, SkuSnapshotDTO> snapshots = loadCheckoutSnapshots(skuIds);
        reserveCheckoutStock(quantityBySku);

        String orderGroupNo = generateOrderGroupNo();
        Map<Long, List<OrderItemPO>> itemsByMerchant = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : quantityBySku.entrySet()) {
            Long skuId = entry.getKey();
            Integer quantity = entry.getValue();
            SkuSnapshotDTO snapshot = snapshots.get(skuId);
            if (snapshot == null || snapshot.getPrice() == null) {
                throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
            }
            if (snapshot.getMerchantId() == null) {
                throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
            }

            BigDecimal lineAmount = normalizeAmount(snapshot.getPrice()
                .multiply(BigDecimal.valueOf(quantity)));

            OrderItemPO itemPO = new OrderItemPO();
            itemPO.setSkuId(skuId);
            itemPO.setSpuId(snapshot.getSpuId());
            itemPO.setMerchantId(snapshot.getMerchantId());
            itemPO.setSkuName(snapshot.getSkuName());
            itemPO.setQuantity(quantity);
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
            po.setReceiverSnapshotJson(toReceiverSnapshotJson(receiver));
            orderManager.save(po);

            for (OrderItemPO itemPO : merchantItems) {
                itemPO.setOrderId(po.getId());
                orderItemManager.save(itemPO);
            }

            Long payId = orderRemoteGateway.createPayment(
                new PayCreateDTO(String.valueOf(po.getId()), userId, merchantId, groupAmount, null)).getData();
            if (payId == null || payId < 0) {
                throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
            }
            po.setPayId(payId);
            orderManager.updateById(po);

            if (mainOrderId == null) {
                mainOrderId = po.getId();
            }
            log.info("订单结算拆单，orderId={}，userId={}，merchantId={}，groupNo={}，items={}，amount={}",
                po.getId(), userId, merchantId, orderGroupNo, merchantItems.size(), groupAmount);
        }

        log.info("订单结算完成，userId={}，groupNo={}，subOrders={}",
            userId, orderGroupNo, itemsByMerchant.size());
        return mainOrderId;
    }


    /**
     * 填充或标记CheckoutItems。
     * @param items 明细列表
     * @return 结果映射
     * @throws BizException 业务校验失败时抛出
     */
    private Map<Long, Integer> mergeCheckoutItems(List<OrderCheckoutItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(OrderCodeEnum.ORDER_ITEMS_EMPTY);
        }
        Map<Long, Integer> quantityBySku = new LinkedHashMap<>(items.size() * 2);
        for (OrderCheckoutItemDTO item : items) {
            if (item == null || item.getSkuId() == null || item.getQuantity() == null
                || item.getQuantity() <= 0) {
                throw new BizException(OrderCodeEnum.ORDER_CHECKOUT_ITEM_INVALID);
            }
            long merged = (long) quantityBySku.getOrDefault(item.getSkuId(), 0)
                + item.getQuantity();
            if (merged > Integer.MAX_VALUE) {
                throw new BizException(OrderCodeEnum.ORDER_CHECKOUT_ITEM_INVALID);
            }
            if (!quantityBySku.containsKey(item.getSkuId())
                && quantityBySku.size() >= MAX_CHECKOUT_SKU_COUNT) {
                throw new BizException(OrderCodeEnum.ORDER_ITEMS_LIMIT_EXCEEDED);
            }
            quantityBySku.put(item.getSkuId(), (int) merged);
        }
        return quantityBySku;
    }


    /**
     * 加载CheckoutSnapshots。
     * @param skuIds SKU ID 列表
     * @return 结果映射
     * @throws BizException 业务校验失败时抛出
     */
    private Map<Long, SkuSnapshotDTO> loadCheckoutSnapshots(List<Long> skuIds) {
        Result<Map<Long, SkuSnapshotDTO>> result = orderRemoteGateway.batchSkuSnapshot(skuIds);
        Map<Long, SkuSnapshotDTO> snapshots = result == null ? null : result.getData();
        if (snapshots == null || snapshots.size() != skuIds.size()
            || !snapshots.keySet().containsAll(skuIds)) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        return snapshots;
    }


    /**
     * 执行Checkout库存操作。
     * @param quantityBySku 数量根据SKU
     * @throws BizException 业务校验失败时抛出
     */
    private void reserveCheckoutStock(Map<Long, Integer> quantityBySku) {
        List<StockReserveDTO> reserveItems = quantityBySku.entrySet().stream()
            .map(entry -> new StockReserveDTO(entry.getKey(), entry.getValue()))
            .toList();
        Result<Boolean> result = orderRemoteGateway.reserveBatch(new StockBatchReserveDTO(reserveItems));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())
            || !Boolean.TRUE.equals(result.getData())) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }
    }


    /**
     * 执行 pay 方法。
     * @param orderId orderId 参数
     */
    public void pay(Long orderId) {
        doPay(getOrder(orderId));
    }


    /**
     * 处理do支付。
     * @param po 持久化对象
     * @throws BizException 业务校验失败时抛出
     */
    private void doPay(OrderPO po) {
        Long orderId = po.getId();
        if (po.getStatus() == OrderStatusEnum.PAID.intCode()) {
            log.info("订单已支付，跳过重复推进，orderId={}", orderId);
            return;
        }

        // 确保订单状态为待支付
        if (po.getStatus() != OrderStatusEnum.PENDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, po.getUserId(), po.getAmount(), LocalDateTime.now().toString());
        eventBus.publishInTx(event, txId -> transactionTemplate.executeWithoutResult(status -> {
            transitStatus(orderId, OrderStatusEnum.PAID, "USER_PAY", null);
            if (txId != null) {
                outboxStore.recordCommit(txId);
            }
        }), e -> isOrderPaid(orderId));
        log.info("订单已支付，orderId={}", orderId);
    }


    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     */
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
     * 校验订单Paid。
     * @param orderId 订单 ID
     * @return 是否满足条件
     */
    private boolean isOrderPaid(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        return po != null && po.getStatus() == OrderStatusEnum.PAID.intCode();
    }

    /**
     * 执行 cancel 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    public void cancel(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
        transition(po, OrderStatusEnum.CANCELED, "USER_CANCEL", "USER_CANCEL", null);
        boolean released = releaseStockForOrder(po);
        markStockReleaseResult(po, released);
        if (!released) {
            log.error("库存释放待处理，orderId={}", orderId);
        }
        log.info("订单已取消，orderId={}，userId={}", orderId, userId);
    }

    /**
     * 执行 ship 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param carrier carrier 参数
     * @param trackingNo trackingNo 参数
     */
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
        log.info("订单已发货，orderId={}，merchantId={}，carrier={}，trackingNo={}",
            orderId, merchantId, carrier, trackingNo);
    }


    /**
     * 执行 merchantInitiateRefund 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     */
    public void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason) {
        merchantInitiateRefund(orderId, merchantId, refundAmount, reason, "R" + orderId);
    }

    /**
     * 执行 merchantInitiateRefund 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     * @param refundRequestNo refundRequestNo 参数
     */
    public void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason,
                                       String refundRequestNo) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        String requestNo = resolveRefundRequestNo(po, refundRequestNo);
        if (po.getStatus() == OrderStatusEnum.REFUNDING.intCode()
            || po.getStatus() == OrderStatusEnum.REFUNDED.intCode()) {
            log.info("商家退款已提交，orderId={}，requestNo={}", orderId, requestNo);
            return;
        }
        OrderStatusEnum current = statusMachine.fromCode(po.getStatus());
        if (current != OrderStatusEnum.PAID && current != OrderStatusEnum.SHIPPED) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        po.setRefundAmount(validateRefundAmount(refundAmount, po));
        orderManager.updateById(po);
        refund(orderId, po.getUserId(), requestNo);
        log.info("商家已发起退款，orderId={}，merchantId={}，amount={}，reason={}",
            orderId, merchantId, refundAmount, reason);
    }

    /**
     * 执行 merchantInitiateRefundByNo 方法。
     * @param refundNo refundNo 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     */
    public void merchantInitiateRefundByNo(String refundNo, Long merchantId,
                                           String refundAmount, String reason) {
        merchantInitiateRefund(parseOrderIdFromRefundNo(refundNo), merchantId,
            refundAmount, reason, refundNo);
    }

    /**
     * 执行 confirm 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    public void confirm(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.COMPLETED, "USER_CONFIRM", null);
        log.info("订单已确认收货，orderId={}，userId={}", orderId, userId);
    }

    /**
     * 执行 refund 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    public void refund(Long orderId, Long userId) {
        refund(orderId, userId, UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 处理退款。
     * @param orderId 订单 ID
     * @param userId 用户 ID
     * @param refundRequestNo 退款Request 编号
     * @throws BizException 业务校验失败时抛出
     */
    private void refund(Long orderId, Long userId, String refundRequestNo) {
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
        po.setRefundRequestNo(resolveRefundRequestNo(po, refundRequestNo));
        transition(po, OrderStatusEnum.REFUNDING, "USER_APPLY_REFUND", null, null);
        log.info("订单退款处理中，orderId={}", orderId);
    }

    /**
     * 处理业务事件。
     * @param dto dto 参数
     */
    public void handleRefundCallback(RefundCallbackDTO dto) {
        if (dto == null || dto.getOrderId() == null || dto.getRefundedAmount() == null
            || dto.getTotalRefundedAmount() == null) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        OrderPO po = getOrder(dto.getOrderId());
        if (po.getPayId() != null && dto.getPayId() != null && !po.getPayId().equals(dto.getPayId())) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (po.getRefundRequestNo() != null && !po.getRefundRequestNo().isBlank()
            && dto.getRefundRequestNo() != null
            && !po.getRefundRequestNo().equals(dto.getRefundRequestNo())) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }

        BigDecimal refundedAmount = normalizeAmount(dto.getRefundedAmount());
        BigDecimal totalRefunded = normalizeAmount(dto.getTotalRefundedAmount());
        BigDecimal currentRefunded = po.getTotalRefundedAmount() == null
            ? BigDecimal.ZERO : po.getTotalRefundedAmount();
        if (totalRefunded.compareTo(currentRefunded) <= 0) {
            log.info("退款回调已忽略，累计退款金额未变化，orderId={}，total={}",
                dto.getOrderId(), currentRefunded);
            return;
        }
        if (refundedAmount.compareTo(BigDecimal.ZERO) <= 0
            || totalRefunded.compareTo(currentRefunded.add(refundedAmount)) != 0) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        BigDecimal payable = po.getPayAmount() != null ? po.getPayAmount() : po.getAmount();
        if (payable == null || totalRefunded.compareTo(payable) > 0) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }

        po.setTotalRefundedAmount(totalRefunded);
        boolean fullyRefunded = Boolean.TRUE.equals(dto.getFullyRefunded())
            || totalRefunded.compareTo(payable) >= 0;
        if (fullyRefunded) {
            transition(po, OrderStatusEnum.REFUNDED, "PAY_REFUND_CALLBACK", null, null);
            log.info("订单已全额退款，orderId={}，totalRefunded={}", po.getId(), totalRefunded);
        }
        else {
            // 部分退款回到退款前状态，并清理本轮退款请求键。
            po.setRefundAmount(null);
            po.setRefundRequestNo(null);
            transition(po, revertTarget(po), "PAY_REFUND_CALLBACK_PARTIAL", null, null);
            log.info("订单已部分退款，orderId={}，totalRefunded={}",
                po.getId(), totalRefunded);
        }
    }


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 业务数值
     */
    @Override
    public Integer getStatus(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po.getStatus();
    }


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @return 业务数值
     */
    @Override
    public Integer getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
    }


    /**
     * 执行 resolveMerchantId 方法。
     * @param ref ref 参数
     * @return 业务数值
     */
    @Override
    public Long resolveMerchantId(String ref) {
        OrderPO po = findOrderByRef(ref);
        return po == null ? null : po.getMerchantId();
    }


    /**
     * 获取相关数据。
     * @param ref ref 参数
     * @return 处理结果
     */
    @Override
    public OrderPaySnapshotDTO getPaySnapshot(String ref) {
        OrderPO po = findOrderByRef(ref);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        BigDecimal payAmount = po.getPayAmount() != null ? po.getPayAmount() : po.getAmount();
        return new OrderPaySnapshotDTO(po.getId(), po.getOrderNo(), po.getUserId(), po.getMerchantId(),
            payAmount, po.getStatus(), po.getPayId());
    }


    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 处理结果
     */
    @Override
    public OrderCommentEligibilityDTO commentEligibility(String orderNo, Long userId, Long spuId) {
        OrderCommentEligibilityDTO notEligible = new OrderCommentEligibilityDTO(false, null, userId, null, null);
        if (orderNo == null || orderNo.isBlank() || userId == null || spuId == null) {
            return notEligible;
        }
        OrderPO po = findOrderByRef(orderNo);
        if (po == null || !userId.equals(po.getUserId())) {
            return notEligible;
        }
        if (!PURCHASED_STATUS_CODES.contains(po.getStatus())) {
            return eligibilityOf(po, false, po.getMerchantId());
        }
        OrderItemPO matched = orderItemManager.getOne(Wrappers.lambdaQuery(OrderItemPO.class)
            .eq(OrderItemPO::getOrderId, po.getId())
            .eq(OrderItemPO::getSpuId, spuId)
            .last("LIMIT 1"));
        if (matched == null) {
            return eligibilityOf(po, false, po.getMerchantId());
        }
        Long merchantId = matched.getMerchantId() != null ? matched.getMerchantId() : po.getMerchantId();
        return eligibilityOf(po, true, merchantId);
    }


    /**
     * 处理eligibilityOf。
     * @param po 持久化对象
     * @param eligible eligible
     * @param merchantId 商户 ID
     * @return 处理结果
     */
    private OrderCommentEligibilityDTO eligibilityOf(OrderPO po, boolean eligible, Long merchantId) {
        return new OrderCommentEligibilityDTO(eligible, po.getId(), po.getUserId(), merchantId, po.getStatus());
    }


    /**
     * 加载订单根据Ref。
     * @param ref ref
     * @return 查询结果
     */
    private OrderPO findOrderByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        OrderPO po = orderManager.getOne(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getOrderNo, ref)
            .last("LIMIT 1"));
        if (po != null) {
            return po;
        }
        if (ref.matches("\\d+")) {
            try {
                po = orderManager.getById(Long.valueOf(ref));
            }
            catch (NumberFormatException e) {
                po = null;
            }
        }
        return po;
    }


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    public OrderPO getDetail(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @return 处理结果
     */
    public OrderPO getDetail(Long orderId) {
        return getOrder(orderId);
    }


    /**
     * 查询列表数据。
     * @param orderId orderId 参数
     * @return 结果列表
     */
    @Override
    public List<OrderItemVO> listItemVO(Long orderId) {
        List<OrderItemPO> items = orderItemManager.list(
            Wrappers.lambdaQuery(OrderItemPO.class).eq(OrderItemPO::getOrderId, orderId));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderItemVO> vos = items.stream()
            .map(po -> OrderItemVO.from(po, minioUtil))
            .collect(java.util.stream.Collectors.toList());
        fillSpuName(vos);
        return vos;
    }


    /**
     * 填充或标记SPUName。
     * @param vos vos
     */
    private void fillSpuName(List<OrderItemVO> vos) {
        List<Long> spuIds = vos.stream().map(OrderItemVO::getSpuId)
            .filter(Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        if (spuIds.isEmpty()) {
            return;
        }
        Result<java.util.Map<Long, SpuSnapshotDTO>> result = orderRemoteGateway.batchSpuSnapshot(spuIds);
        java.util.Map<Long, SpuSnapshotDTO> snapshots =
            (result != null && result.getData() != null) ? result.getData() : Collections.emptyMap();
        for (OrderItemVO vo : vos) {
            SpuSnapshotDTO snap = snapshots.get(vo.getSpuId());
            if (snap != null) {
                vo.setSpuName(snap.getTitle());
            }
        }
    }


    /**
     * 执行 merchantClose 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     */
    public void merchantClose(Long orderId, Long merchantId) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
        transition(po, OrderStatusEnum.CANCELED, "MERCHANT_CLOSE", "MERCHANT_CLOSE", null);
        boolean released = releaseStockForOrder(po);
        markStockReleaseResult(po, released);
        if (!released) {
            log.error("库存释放待处理，orderId={}", orderId);
        }
        log.info("订单已由商家关闭，orderId={}，merchantId={}", orderId, merchantId);
    }


    /**
     * 删除业务数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
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
        log.info("订单已删除，orderId={}，userId={}", orderId, userId);
    }


    /**
     * 执行 platformClose 方法。
     * @param orderId orderId 参数
     */
    public void platformClose(Long orderId) {
        OrderPO po = getOrder(orderId);
        po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
        transition(po, OrderStatusEnum.CANCELED, "PLATFORM_CLOSE", "PLATFORM_CLOSE", null);
        boolean released = releaseStockForOrder(po);
        markStockReleaseResult(po, released);
        if (!released) {
            log.error("库存释放待处理，orderId={}", orderId);
        }
        log.info("订单已由平台关闭，orderId={}", orderId);
    }


    /**
     * 执行 pendingCount 方法。
     * @param merchantId merchantId 参数
     * @return 业务数值
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

    /**
     * 获取订单。
     * @param orderId 订单 ID
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
    private OrderPO getOrder(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }


    /**
     * 执行库存For订单操作。
     * @param po 持久化对象
     * @return 是否满足条件
     */
    private boolean releaseStockForOrder(OrderPO po) {
        List<OrderItemPO> items = orderItemManager.list(
            Wrappers.lambdaQuery(OrderItemPO.class).eq(OrderItemPO::getOrderId, po.getId()));
        if (!items.isEmpty()) {
            for (OrderItemPO item : items) {
                Result<Boolean> result = orderRemoteGateway.release(
                    new StockReserveDTO(item.getSkuId(), item.getQuantity(), po.getOrderNo()));
                if (result == null || !CommonCode.SUCCESS.equals(result.getCode())
                    || !Boolean.TRUE.equals(result.getData())) {
                    return false;
                }
            }
            return true;
        }
        if (po.getSkuId() != null && po.getQuantity() != null) {
            Result<Boolean> result = orderRemoteGateway.release(
                new StockReserveDTO(po.getSkuId(), po.getQuantity(), po.getOrderNo()));
            return result != null && CommonCode.SUCCESS.equals(result.getCode())
                && Boolean.TRUE.equals(result.getData());
        }
        return true;
    }


    /**
     * 填充或标记库存释放Result。
     * @param po 持久化对象
     * @param released released
     */
    private void markStockReleaseResult(OrderPO po, boolean released) {
        po.setStockReleaseStatus((released
            ? StockReleaseStatusEnum.RELEASED : StockReleaseStatusEnum.PENDING).intCode());
        orderManager.updateById(po);
    }


    /**
     * 处理transit状态。
     * @param orderId 订单 ID
     * @param target 目标对象
     * @param triggerSource triggerSource
     * @param closeReason 关闭Reason
     */
    private void transitStatus(Long orderId, OrderStatusEnum target, String triggerSource, String closeReason) {
        transition(getOrder(orderId), target, triggerSource, closeReason, null);
    }


    /**
     * 处理transition。
     * @param po 持久化对象
     * @param target 目标对象
     * @param triggerSource triggerSource
     * @param closeReason 关闭Reason
     * @param note note
     * @throws BizException 业务校验失败时抛出
     */
    private void transition(OrderPO po, OrderStatusEnum target, String triggerSource, String closeReason, String note) {
        Integer fromStatus = po.getStatus();
        statusMachine.transit(po, target);
        applyTargetTimestamps(po, target, closeReason);
        if (!orderManager.updateById(po)) {
            // 并发下后写者更新 0 行：目标状态已被先行事务推进，拒绝继续执行副作用
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        saveStatusLog(po, fromStatus, target, triggerSource, note);
    }


    /**
     * 处理revertTarget。
     * @param po 持久化对象
     * @return 处理结果
     */
    private OrderStatusEnum revertTarget(OrderPO po) {
        Integer fromStatus = po.getRefundFromStatus();
        return fromStatus != null ? statusMachine.fromCode(fromStatus) : OrderStatusEnum.PAID;
    }


    /**
     * 执行TargetTimestamps逻辑。
     * @param po 持久化对象
     * @param target 目标对象
     * @param closeReason 关闭Reason
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
     * 创建状态日志。
     * @param po 持久化对象
     * @param fromStatus 从状态
     * @param toStatus 转换状态
     * @param triggerSource triggerSource
     * @param note note
     */
    private void saveStatusLog(OrderPO po, Integer fromStatus, OrderStatusEnum toStatus,
                               String triggerSource, String note) {
        OrderStatusLogPO log = new OrderStatusLogPO();
        log.setOrderId(po.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus.intCode());
        log.setTriggerSource(triggerSource);
        log.setNote(note);
        orderStatusLogManager.save(log);
    }


    /**
     * 构建或生成订单编号。
     * @return 编号或编码
     */
    private String generateOrderNo() {
        return "OD" + requireGlobalId("ORDER");
    }


    /**
     * 构建或生成订单Group编号。
     * @return 编号或编码
     */
    private String generateOrderGroupNo() {
        return "OG" + requireGlobalId("ORDER_GROUP");
    }


    /**
     * 处理requireGlobalID。
     * @param bizTag bizTag
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private Long requireGlobalId(String bizTag) {
        try {
            Result<Long> result = orderRemoteGateway.nextId(bizTag);
            if (result != null && CommonCode.SUCCESS.equals(result.getCode()) && result.getData() != null) {
                return result.getData();
            }
            log.error("id-service 返回失败，bizTag={}，result={}", bizTag, result);
        }
        catch (Exception e) {
            log.error("id-service nextId 失败，bizTag={}", bizTag, e);
        }
        throw new BizException(OrderCodeEnum.ORDER_ID_GENERATE_FAIL);
    }


    /**
     * 校验退款金额。
     * @param refundAmount 退款金额
     * @param po 持久化对象
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private BigDecimal validateRefundAmount(String refundAmount, OrderPO po) {
        BigDecimal refundAmt;
        try {
            refundAmt = new BigDecimal(refundAmount);
        }
        catch (NumberFormatException | NullPointerException e) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        BigDecimal payAmount = po.getPayAmount() != null ? po.getPayAmount() : po.getAmount();
        BigDecimal totalRefunded = po.getTotalRefundedAmount() == null
            ? BigDecimal.ZERO : po.getTotalRefundedAmount();
        BigDecimal remainingAmount = payAmount == null
            ? null : payAmount.subtract(totalRefunded);
        if (payAmount == null || refundAmt.compareTo(BigDecimal.ZERO) <= 0
            || remainingAmount.compareTo(BigDecimal.ZERO) <= 0
            || refundAmt.compareTo(remainingAmount) > 0) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        try {
            return refundAmt.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        }
        catch (ArithmeticException e) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
    }


    /**
     * 转换或解析快照Json。
     * @param value 值
     * @return 处理结果
     */
    private String toSnapshotJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException e) {
            log.warn("序列化订单快照失败", e);
            return null;
        }
    }


    /**
     * 转换或解析收货人快照Json。
     * @param receiver 收货人信息
     * @return 处理结果
     */
    private String toReceiverSnapshotJson(ReceiverDTO receiver) {
        return receiver == null ? null : toSnapshotJson(receiver);
    }


    /**
     * 计算金额。
     * @param itemPOs itemPOs
     * @return 处理结果
     */
    private BigDecimal calcAmount(List<OrderItemPO> itemPOs) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemPO item : itemPOs) {
            total = total.add(item.getAmount());
        }
        return normalizeAmount(total);
    }


    /**
     * 构建或生成金额。
     * @param amount 金额
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private static BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        }
        catch (ArithmeticException e) {
            throw new BizException(OrderCodeEnum.ORDER_AMOUNT_SCALE_INVALID);
        }
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
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
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<OrderVO> pageVO(OrderPageDTO dto) {
        return page(dto).map(OrderVO::from);
    }


    /**
     * 执行 detailVO 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public OrderVO detailVO(Long orderId, Long userId) {
        OrderVO vo = OrderVO.from(getDetail(orderId, userId));
        vo.setItems(listItemVO(orderId));
        return vo;
    }


    /**
     * 执行 platformDetailVO 方法。
     * @param orderId orderId 参数
     * @return 处理结果
     */
    @Override
    public OrderVO platformDetailVO(Long orderId) {
        return OrderVO.from(getDetail(orderId));
    }


    /**
     * 执行 merchantPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<OrderVO> merchantPageVO(OrderPageDTO dto) {
        CursorPageVO<OrderVO> result = page(dto).map(OrderVO::from);
        fillItemSummary(result.getRecords());
        fillSkuName(result.getRecords());
        fillRejectReason(result.getRecords());
        return result;
    }


    /**
     * 填充或标记Item汇总。
     * @param vos vos
     */
    private void fillItemSummary(List<OrderVO> vos) {
        List<Long> orderIds = vos.stream().map(OrderVO::getId).toList();
        List<OrderItemPO> items = orderItemManager.list(
            Wrappers.lambdaQuery(OrderItemPO.class).in(OrderItemPO::getOrderId, orderIds));
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, List<OrderItemPO>> byOrder = items.stream()
            .collect(Collectors.groupingBy(OrderItemPO::getOrderId));
        Map<Long, String> spuTitles = fetchSpuTitles(byOrder);
        for (OrderVO vo : vos) {
            List<OrderItemPO> orderItems = byOrder.getOrDefault(vo.getId(), Collections.emptyList());
            if (orderItems.isEmpty()) {
                continue;
            }
            if (vo.getQuantity() == null) {
                vo.setQuantity(orderItems.stream()
                    .map(OrderItemPO::getQuantity).filter(Objects::nonNull)
                    .mapToInt(Integer::intValue).sum());
            }
            vo.setSkuName(buildItemDisplay(orderItems, spuTitles));
        }
    }


    /**
     * 处理fetchSPUTitles。
     * @param byOrder 根据订单
     * @return 结果映射
     */
    private Map<Long, String> fetchSpuTitles(Map<Long, List<OrderItemPO>> byOrder) {
        Set<Long> spuIds = byOrder.values().stream()
            .flatMap(List::stream)
            .map(OrderItemPO::getSpuId).filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (spuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<java.util.Map<Long, SpuSnapshotDTO>> result =
            orderRemoteGateway.batchSpuSnapshot(new ArrayList<>(spuIds));
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        java.util.Map<Long, String> titles = new HashMap<>();
        result.getData().forEach((spuId, snap) -> {
            if (snap != null && snap.getTitle() != null) {
                titles.put(spuId, snap.getTitle());
            }
        });
        return titles;
    }


    /**
     * 构建或生成ItemDisplay。
     * @param orderItems 订单Items
     * @param spuTitles SPUTitles
     * @return 处理结果
     */
    private String buildItemDisplay(List<OrderItemPO> orderItems, Map<Long, String> spuTitles) {
        List<OrderItemPO> head = orderItems;
        boolean truncated = false;
        if (orderItems.size() > 2) {
            head = orderItems.subList(0, 2);
            truncated = true;
        }
        String joined = head.stream()
            .map(item -> buildItemSegment(item, spuTitles))
            .filter(Objects::nonNull)
            .collect(Collectors.joining("、"));
        return truncated ? joined + "…" : joined;
    }


    /**
     * 构建或生成ItemSegment。
     * @param item 明细项
     * @param spuTitles SPUTitles
     * @return 处理结果
     */
    private String buildItemSegment(OrderItemPO item, Map<Long, String> spuTitles) {
        String title = item.getSpuId() == null ? null : spuTitles.get(item.getSpuId());
        Integer qty = item.getQuantity();
        String tail = item.getSkuName();
        if (qty != null) {
            tail = (tail == null ? "" : tail + " * ") + qty;
        }
        tail = (tail == null || tail.isEmpty()) ? null : tail;
        if (title == null) {
            return tail;
        }
        return tail == null ? title : title + " | " + tail;
    }


    /**
     * 执行 merchantDetailVO 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public OrderVO merchantDetailVO(Long orderId, Long merchantId) {
        OrderPO po = orderManager.getOne(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getId, orderId)
            .eq(OrderPO::getMerchantId, merchantId));
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        OrderVO vo = OrderVO.from(po);
        fillItemSummary(List.of(vo));
        fillSkuName(List.of(vo));
        fillRejectReason(List.of(vo));
        return vo;
    }


    /**
     * 填充或标记SKUName。
     * @param vos vos
     */
    private void fillSkuName(List<OrderVO> vos) {
        List<Long> skuIds = vos.stream().map(OrderVO::getSkuId).filter(Objects::nonNull).distinct().toList();
        if (skuIds.isEmpty()) {
            return;
        }
        Result<Map<Long, SkuSnapshotDTO>> result = orderRemoteGateway.batchSkuSnapshot(skuIds);
        Map<Long, SkuSnapshotDTO> snapshots =
            (result != null && result.getData() != null) ? result.getData() : Collections.emptyMap();
        for (OrderVO vo : vos) {
            if (vo.getSkuName() != null) {
                continue;
            }
            SkuSnapshotDTO snap = snapshots.get(vo.getSkuId());
            if (snap != null) {
                vo.setSkuName(snap.getSkuName());
            }
        }
    }


    /**
     * 填充或标记RejectReason。
     * @param vos vos
     */
    private void fillRejectReason(List<OrderVO> vos) {
        List<Long> orderIds = vos.stream().map(OrderVO::getId).toList();
        if (orderIds.isEmpty()) {
            return;
        }
        List<OrderStatusLogPO> logs = orderStatusLogManager.list(
            Wrappers.lambdaQuery(OrderStatusLogPO.class)
                .in(OrderStatusLogPO::getOrderId, orderIds)
                .eq(OrderStatusLogPO::getTriggerSource, "MERCHANT_REJECT_REFUND")
                .isNotNull(OrderStatusLogPO::getNote)
                .orderByDesc(OrderStatusLogPO::getId));
        Map<Long, String> reasonByOrder = new HashMap<>();
        for (OrderStatusLogPO log : logs) {
            reasonByOrder.putIfAbsent(log.getOrderId(), log.getNote());
        }
        for (OrderVO vo : vos) {
            vo.setRejectReason(reasonByOrder.get(vo.getId()));
        }
    }


    /**
     * 执行 exportList 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 结果列表
     */
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
     * 执行 exportVOList 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 结果列表
     */
    @Override
    public List<OrderExportVO> exportVOList(Long merchantId, OrderPageDTO dto) {
        return exportList(merchantId, dto).stream().map(OrderExportVO::from).toList();
    }


    /**
     * 执行 platformExportList 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
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
     * 执行 platformExportVOList 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    @Override
    public List<OrderExportVO> platformExportVOList(OrderPageDTO dto) {
        return platformExportList(dto).stream().map(OrderExportVO::from).toList();
    }

    /**
     * 执行 platformExportVOList 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    @Override
    public List<OrderExportVO> platformExportVOList(String startDate, String endDate) {
        OrderPageDTO dto = new OrderPageDTO();
        dto.setStartTime(parseStart(startDate));
        dto.setEndTime(parseEnd(endDate));
        return platformExportVOList(dto);
    }


    /**
     * 转换或解析状态编码。
     * @param status 状态
     * @return 处理结果
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
     * 执行 statistics 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public OrderStatisticsVO statistics(OrderPageDTO dto) {
        OrderStatisticsResultDTO result =
            orderManager.statistics(dto.getMerchantId(), dto.getStartTime(), dto.getEndTime());
        if (result == null) {
            return null;
        }
        return new OrderStatisticsVO(
            result.getTotalOrderCount(), result.getTotalAmount(), result.getRefundAmount(),
            result.getPendingCount(), result.getPaidCount(), result.getShippedCount(),
            result.getReceivedCount(), result.getCancelledCount(), result.getRefundingCount(),
            result.getRefundedCount());
    }


    /**
     * 执行 orderSummary 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public OrderSummaryVO orderSummary(String startDate, String endDate, String merchantId) {
        OrderSummaryResultDTO result = orderManager.summary(
            parseMerchantId(merchantId), parseStart(startDate), parseEnd(endDate));
        if (result == null) {
            return null;
        }
        return new OrderSummaryVO(
            result.getTotalCount(), result.getTotalAmount(), result.getPaidAmount(),
            result.getPendingAmount(), result.getRefundAmount(), result.getNetIncome());
    }


    /**
     * 执行 orderTrend 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public OrderTrendVO orderTrend(String startDate, String endDate, String merchantId) {
        List<OrderTrendPointDTO> points = orderManager.trend(
            parseMerchantId(merchantId), parseStart(startDate), parseEnd(endDate));
        List<OrderTrendPointVO> vos = points == null ? Collections.emptyList()
            : points.stream()
                .map(p -> new OrderTrendPointVO(p.getDate(), p.getCount(), p.getAmount()))
                .toList();
        return new OrderTrendVO(vos);
    }


    /**
     * 转换或解析商户ID。
     * @param merchantId 商户 ID
     * @return 处理结果
     */
    private Long parseMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(merchantId.trim());
        }
        catch (NumberFormatException e) {
            log.warn("订单汇总中的 merchantId 非法，按全平台处理。merchantId={}", merchantId);
            return null;
        }
    }


    /**
     * 转换或解析启动。
     * @param date 日期
     * @return 处理结果
     */
    private LocalDateTime parseStart(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay();
        }
        catch (Exception e) {
            log.warn("订单汇总中的 startDate 非法，忽略边界。date={}", date);
            return null;
        }
    }


    /**
     * 转换或解析End。
     * @param date 日期
     * @return 处理结果
     */
    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atTime(LocalTime.MAX);
        }
        catch (Exception e) {
            log.warn("订单汇总中的 endDate 非法，忽略边界。date={}", date);
            return null;
        }
    }


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public OrderStatusCountsVO countStatusByUser(Long userId) {
        List<OrderStatusCountDTO> rows = orderManager.countByStatus(userId);
        OrderStatusCountsVO vo = new OrderStatusCountsVO();
        for (OrderStatusCountDTO row : rows) {
            if (row.getStatus() == null || row.getCnt() == null) {
                continue;
            }
            long count = row.getCnt();
            switch (row.getStatus()) {
                case 1 -> vo.setPendingCount(count);
                case 2 -> vo.setPaidCount(count);
                case 3 -> vo.setShippedCount(count);
                case 4 -> vo.setCompletedCount(count);
                case 5 -> vo.setCanceledCount(count);
                case 6 -> vo.setRefundingCount(count);
                case 7 -> vo.setRefundedCount(count);
            }
        }
        return vo;
    }


    /**
     * 执行 reviewRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     */
    public void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason) {
        reviewRefund(orderId, approved, merchantId, reason, null);
    }

    /**
     * 执行 reviewRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     * @param refundRequestNo refundRequestNo 参数
     */
    public void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason,
                             String refundRequestNo) {
        OrderPO po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_REFUNDING);
        }
        if (!approved) {
            // 驳回原因写入状态日志 note，供商家端列表回显
            transition(po, revertTarget(po), "MERCHANT_REJECT_REFUND", null, reason);
            log.info("商家已拒绝退款，orderId={}，merchantId={}", orderId, merchantId);
            return;
        }
        // 审核通过才调支付网关真实退款，避免"先退钱后审核"；状态迁移由支付回调 handleRefundCallback 驱动
        boolean requestNoMissing = po.getRefundRequestNo() == null || po.getRefundRequestNo().isBlank();
        String requestNo = resolveRefundRequestNo(po, refundRequestNo);
        if (requestNoMissing) {
            orderManager.updateById(po);
        }
        requestPayRefund(po, requestNo);
        log.info("商家已同意退款，orderId={}，merchantId={}", orderId, merchantId);
    }

    /**
     * 执行 reviewRefundByNo 方法。
     * @param refundNo refundNo 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     */
    public void reviewRefundByNo(String refundNo, boolean approved, Long merchantId, String reason) {
        reviewRefund(parseOrderIdFromRefundNo(refundNo), approved, merchantId, reason, refundNo);
    }


    /**
     * 转换或解析订单ID从退款编号。
     * @param refundNo 退款 编号
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private Long parseOrderIdFromRefundNo(String refundNo) {
        if (refundNo == null || refundNo.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "退款单号不能为空");
        }
        String tail = refundNo.startsWith("R") ? refundNo.substring(1) : refundNo;
        try {
            return Long.parseLong(tail);
        }
        catch (NumberFormatException e) {
            throw new BizException(CommonCode.PARAM_INVALID, "退款单号格式错误：" + refundNo);
        }
    }


    /**
     * 执行 arbitrateRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     */
    public void arbitrateRefund(Long orderId, boolean approved) {
        OrderPO po = getOrder(orderId);
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_REFUNDING);
        }
        if (approved) {
            requestPayRefund(po, resolveRefundRequestNo(po, null));
            log.info("退款仲裁已同意并提交，orderId={}", orderId);
            return;
        }
        transitStatus(orderId, revertTarget(po), "PLATFORM_ARBITRATE_REJECT", null);
        log.info("退款仲裁已拒绝，orderId={}", orderId);
    }


    /**
     * 处理request支付退款。
     * @param po 持久化对象
     * @param refundRequestNo 退款Request 编号
     * @throws BizException 业务校验失败时抛出
     */
    private void requestPayRefund(OrderPO po, String refundRequestNo) {
        BigDecimal refundAmount = po.getRefundAmount() != null ? po.getRefundAmount() : po.getAmount();
        if (refundAmount == null) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        BigDecimal refundAmt = normalizeAmount(refundAmount);
        Result<Boolean> refundResult = orderRemoteGateway.refund(
            new RefundCreateDTO(po.getPayId(), refundAmt, null, refundRequestNo));
        if (refundResult == null
            || !CommonCode.SUCCESS.equals(refundResult.getCode())
            || !Boolean.TRUE.equals(refundResult.getData())) {
            log.error("支付退款失败，orderId={}，payId={}", po.getId(), po.getPayId());
            throw new BizException(OrderCodeEnum.ORDER_REFUND_FAIL);
        }
    }


    /**
     * 转换或解析退款Request编号。
     * @param po 持久化对象
     * @param candidate candidate
     * @return 编号或编码
     * @throws BizException 业务校验失败时抛出
     */
    private String resolveRefundRequestNo(OrderPO po, String candidate) {
        String current = po.getRefundRequestNo();
        if (current != null && !current.isBlank()) {
            if (candidate != null && !candidate.isBlank() && !current.equals(candidate)) {
                throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
            }
            return current;
        }
        String resolved = candidate == null || candidate.isBlank()
            ? "R" + po.getId()
            : candidate;
        po.setRefundRequestNo(resolved);
        return resolved;
    }


    /**
     * 执行 queryLogistics 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 结果列表
     */
    @Override
    public List<OrderLogisticsVO> queryLogistics(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        List<OrderLogisticsPO> nodes = orderLogisticsManager.list(
            Wrappers.lambdaQuery(OrderLogisticsPO.class)
                .eq(OrderLogisticsPO::getOrderId, orderId)
                .orderByAsc(OrderLogisticsPO::getCreateTime));
        return nodes.stream().map(this::toLogisticsVO).collect(Collectors.toList());
    }

    /**
     * 转换或解析物流VO。
     * @param po 持久化对象
     * @return 视图对象
     */
    private OrderLogisticsVO toLogisticsVO(OrderLogisticsPO po) {
        OrderLogisticsVO vo = new OrderLogisticsVO();
        vo.setNode(po.getNode());
        vo.setDescription(po.getDescription());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    @Override
    public int scanExpiredOrders() {
        LocalDateTime expireThreshold = LocalDateTime.now().minusMinutes(PAY_EXPIRE_MINUTES);
        int count = 0;
        long lastId = 0L;
        for (int batch = 0; batch < SCAN_MAX_BATCHES; batch++) {
            List<OrderPO> expired = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
                .eq(OrderPO::getStatus, OrderStatusEnum.PENDING.intCode())
                .and(w -> w.isNull(OrderPO::getPayExpireAt).lt(OrderPO::getCreateTime, expireThreshold)
                    .or().lt(OrderPO::getPayExpireAt, expireThreshold))
                .gt(OrderPO::getId, lastId)
                .orderByAsc(OrderPO::getId)
                .last("LIMIT " + SCAN_BATCH_SIZE));
            if (expired.isEmpty()) {
                break;
            }
            for (OrderPO po : expired) {
                try {
                    po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
                    transition(po, OrderStatusEnum.CANCELED, "SYSTEM_TIMEOUT", "SYSTEM_TIMEOUT", null);
                    boolean released = releaseStockForOrder(po);
                    markStockReleaseResult(po, released);
                    if (!released) {
                        log.error("库存释放待处理，orderId={}", po.getId());
                    }
                    count++;
                }
                catch (Exception e) {
                    log.warn("scanExpiredOrders 取消失败，orderId={}，err={}", po.getId(), e.getMessage());
                }
            }
            lastId = expired.get(expired.size() - 1).getId();
            if (expired.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
        if (count > 0) {
            log.info("scanExpiredOrders 已取消 {} 个订单", count);
        }
        return count;
    }


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    @Override
    public int scanStockReleasePending() {
        List<OrderPO> pending = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
            .eq(OrderPO::getStatus, OrderStatusEnum.CANCELED.intCode())
            .eq(OrderPO::getStockReleaseStatus, StockReleaseStatusEnum.PENDING.intCode())
            .orderByAsc(OrderPO::getUpdateTime)
            .orderByAsc(OrderPO::getId)
            .last("LIMIT " + SCAN_BATCH_SIZE));
        int count = 0;
        for (OrderPO po : pending) {
            if (releaseStockForOrder(po)) {
                markStockReleaseResult(po, true);
                count++;
            }
            else {
                // 失败时刷新 updateTime，让该订单排到队尾，避免故障单长期占据批次造成饥饿
                markStockReleaseResult(po, false);
                log.error("库存释放重试失败，orderId={}", po.getId());
            }
        }
        return count;
    }


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    @Override
    public int scanAutoConfirm() {
        LocalDateTime confirmThreshold = LocalDateTime.now().minusDays(AUTO_CONFIRM_DAYS);
        int count = 0;
        long lastId = 0L;
        for (int batch = 0; batch < SCAN_MAX_BATCHES; batch++) {
            List<OrderPO> overdue = orderManager.list(Wrappers.lambdaQuery(OrderPO.class)
                .eq(OrderPO::getStatus, OrderStatusEnum.SHIPPED.intCode())
                .lt(OrderPO::getShippedAt, confirmThreshold)
                .gt(OrderPO::getId, lastId)
                .orderByAsc(OrderPO::getId)
                .last("LIMIT " + SCAN_BATCH_SIZE));
            if (overdue.isEmpty()) {
                break;
            }
            for (OrderPO po : overdue) {
                try {
                    transitStatus(po.getId(), OrderStatusEnum.COMPLETED, "SYSTEM_AUTO_CONFIRM", null);
                    count++;
                }
                catch (Exception e) {
                    log.warn("scanAutoConfirm 确认失败，orderId={}，err={}", po.getId(), e.getMessage());
                }
            }
            lastId = overdue.get(overdue.size() - 1).getId();
            if (overdue.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
        if (count > 0) {
            log.info("scanAutoConfirm 已完成 {} 个订单", count);
        }
        return count;
    }
}
