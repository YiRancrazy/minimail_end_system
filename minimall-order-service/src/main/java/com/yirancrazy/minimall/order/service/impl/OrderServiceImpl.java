package com.yirancrazy.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusMachine;
import com.yirancrazy.minimall.order.constant.StockReleaseStatusEnum;
import com.yirancrazy.minimall.order.dto.CheckoutResultDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDto;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDto;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDto;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDto;
import com.yirancrazy.minimall.order.dto.ReceiverDto;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPo;
import com.yirancrazy.minimall.order.entity.OrderItemPo;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPo;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.entity.OrderStatusLogPo;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.manager.OrderItemManager;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OrderStatusLogManager;
import com.yirancrazy.minimall.order.service.OrderOutboxService;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderExportVo;
import com.yirancrazy.minimall.order.vo.OrderItemVo;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVo;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVo;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVo;
import com.yirancrazy.minimall.order.vo.OrderSummaryVo;
import com.yirancrazy.minimall.order.vo.OrderTrendPointVo;
import com.yirancrazy.minimall.order.vo.OrderTrendVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import io.seata.spring.annotation.GlobalTransactional;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现订单创建、状态流转、退款、查询、统计、导出和补偿扫描核心业务。
 *   业务边界：通过本地事务、状态机、订单发件箱和远程网关协调库存、支付、购物车及商品快照；创建和结算使用全局事务边界。
 *   使用场景：用户、商家、平台、内部控制器和定时任务的主要业务实现。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
    private static final Set<Integer> PURCHASED_STATUS_CODES =
            Set.of(
                    OrderStatusEnum.PAID.intCode(),
                    OrderStatusEnum.SHIPPED.intCode(),
                    OrderStatusEnum.COMPLETED.intCode());

    private final OrderManager orderManager;
    private final OrderItemManager orderItemManager;
    private final OrderLogisticsManager orderLogisticsManager;
    private final OrderStatusLogManager orderStatusLogManager;
    private final OrderRemoteGateway orderRemoteGateway;
    private final OrderOutboxService orderOutboxService;
    private final TransactionTemplate transactionTemplate;
    private final OrderStatusMachine statusMachine;
    private final ObjectMapper objectMapper;
    private final MinioUtil minioUtil;
    private final CartCleanupTaskManager cartCleanupTaskManager;

    /**
     * 创建订单核心服务及其依赖。
     *
     * <p>注入订单、明细、物流、状态日志、远程网关、发件箱、事务模板、状态机、JSON、MinIO 和清理任务依赖。
     *
     * @param orderManager 订单主记录持久化与聚合查询入口
     * @param orderItemManager 订单明细持久化入口
     * @param orderLogisticsManager 订单物流节点持久化入口
     * @param orderStatusLogManager 订单状态迁移日志持久化入口
     * @param orderRemoteGateway 商品、购物车、库存、支付和 ID 服务的远程调用边界
     * @param orderOutboxService 订单支付事件的事务性发件箱写入组件
     * @param transactionTemplate 状态迁移使用的编程式事务模板
     * @param statusMachine 订单状态迁移规则校验器
     * @param objectMapper 订单快照和清理任务载荷的 JSON 序列化器
     * @param minioUtil SKU 图片对象地址解析工具
     * @param cartCleanupTaskManager 购物车异步清理任务持久化入口
     */
    public OrderServiceImpl(
            OrderManager orderManager,
            OrderItemManager orderItemManager,
            OrderLogisticsManager orderLogisticsManager,
            OrderStatusLogManager orderStatusLogManager,
            OrderRemoteGateway orderRemoteGateway,
            OrderOutboxService orderOutboxService,
            TransactionTemplate transactionTemplate,
            OrderStatusMachine statusMachine,
            ObjectMapper objectMapper,
            MinioUtil minioUtil,
            CartCleanupTaskManager cartCleanupTaskManager) {
        this.orderManager = orderManager;
        this.orderItemManager = orderItemManager;
        this.orderLogisticsManager = orderLogisticsManager;
        this.orderStatusLogManager = orderStatusLogManager;
        this.orderRemoteGateway = orderRemoteGateway;
        this.orderOutboxService = orderOutboxService;
        this.transactionTemplate = transactionTemplate;
        this.statusMachine = statusMachine;
        this.objectMapper = objectMapper;
        this.minioUtil = minioUtil;
        this.cartCleanupTaskManager = cartCleanupTaskManager;
    }

    /**
     * 创建单个 SKU 订单并完成库存预占和支付流水初始化。
     *
     * <p>读取服务端 SKU 快照，按最终订单号预占库存，保存订单及明细快照，再创建支付流水；四参数版本把收货信息写入快照。该方法处于全局事务边界内。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param skuId SKU ID，用于读取商品快照或执行库存操作
     * @param quantity 购买数量，必须为正整数
     * @return 新订单主键。
     * @throws BizException SKU 快照缺失、库存不足、ID 生成失败或支付流水创建失败。
     */
    @Override
    public Long create(Long userId, Long skuId, Integer quantity) {
        return create(userId, skuId, quantity, null);
    }

    /**
     * 创建单个 SKU 订单并完成库存预占和支付流水初始化。
     *
     * <p>读取服务端 SKU 快照，按最终订单号预占库存，保存订单及明细快照，再创建支付流水；四参数版本把收货信息写入快照。该方法处于全局事务边界内。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param skuId SKU ID，用于读取商品快照或执行库存操作
     * @param quantity 购买数量，必须为正整数
     * @param receiver 收货信息快照
     * @return 新订单主键。
     * @throws BizException SKU 快照缺失、库存不足、ID 生成失败或支付流水创建失败。
     */
    @Override
    @GlobalTransactional
    public Long create(Long userId, Long skuId, Integer quantity, ReceiverDto receiver) {

        // 获取商品快照
        SkuSnapshotDto snapshot = orderRemoteGateway.skuSnapshot(skuId).getData();
        if (snapshot == null || snapshot.getPrice() == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }

        // 统一小数位数
        final BigDecimal amount =
                normalizeAmount(snapshot.getPrice().multiply(BigDecimal.valueOf(quantity)));

        String orderNo = generateOrderNo();
        // 锁库存
        Boolean reserved =
                orderRemoteGateway.reserve(new StockReserveDto(skuId, quantity, orderNo)).getData();
        if (reserved == null || !reserved) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }

        // 创建订单
        OrderPo po = new OrderPo();
        po.setOrderNo(orderNo);
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
        OrderItemPo itemPo = new OrderItemPo();
        itemPo.setOrderId(po.getId());
        itemPo.setSpuId(snapshot.getSpuId());
        itemPo.setMerchantId(snapshot.getMerchantId());
        itemPo.setSkuId(skuId);
        itemPo.setSkuName(snapshot.getSkuName());
        itemPo.setQuantity(quantity);
        itemPo.setUnitPrice(snapshot.getPrice());
        itemPo.setAmount(amount);
        itemPo.setSubtotalAmount(amount);
        itemPo.setDiscountAmount(BigDecimal.ZERO);
        itemPo.setPayAmount(amount);
        itemPo.setRefundStatus(REFUND_STATUS_NONE);
        orderItemManager.save(itemPo);

        // 创建支付流水：支付服务在 C 端无商户上下文，按订单归属商户收款
        Long payId =
                orderRemoteGateway
                        .createPayment(new PayCreateDto(String.valueOf(po.getId()), userId, 0L, amount, null))
                        .getData();
        if (payId == null || payId < 0) {
            throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
        }
        po.setPayId(payId);
        orderManager.updateById(po);

        log.info("订单已创建，orderId={}，userId={}，amount={}，payId={}", po.getId(), userId, amount, payId);
        return po.getId();
    }

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    @Override
    public Long checkout(Long userId, List<OrderCheckoutItemDto> items) {
        return checkout(userId, items, null);
    }

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @param receiver 收货信息快照
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    @Override
    public Long checkout(Long userId, List<OrderCheckoutItemDto> items, ReceiverDto receiver) {
        return checkout(userId, items, List.of(), receiver, null).getOrderId();
    }

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @param cleanupItems 结算成功后待清理的购物车条目快照
     * @param receiver 收货信息快照
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    @Override
    @GlobalTransactional
    public CheckoutResultDto checkout(
            Long userId,
            List<OrderCheckoutItemDto> items,
            List<CartCheckoutItemDto> cleanupItems,
            ReceiverDto receiver,
            String idempotencyKey) {
        Map<Long, Integer> quantityBySku = mergeCheckoutItems(items);
        List<Long> skuIds = new ArrayList<>(quantityBySku.keySet());
        Map<Long, SkuSnapshotDto> snapshots = loadCheckoutSnapshots(skuIds);
        final Long merchantId = resolveCheckoutMerchantId(quantityBySku, snapshots);
        String orderNo = generateOrderNo();
        Map<Long, String> orderNoBySku = mapCheckoutOrderNo(quantityBySku, orderNo);
        reserveCheckoutStock(quantityBySku, orderNoBySku);

        final String orderGroupNo = generateOrderGroupNo();
        List<OrderItemPo> orderItems = new ArrayList<>(quantityBySku.size());
        for (Map.Entry<Long, Integer> entry : quantityBySku.entrySet()) {
            Long skuId = entry.getKey();
            Integer quantity = entry.getValue();
            SkuSnapshotDto snapshot = snapshots.get(skuId);

            BigDecimal lineAmount =
                    normalizeAmount(snapshot.getPrice().multiply(BigDecimal.valueOf(quantity)));

            OrderItemPo itemPo = new OrderItemPo();
            itemPo.setSkuId(skuId);
            itemPo.setSpuId(snapshot.getSpuId());
            itemPo.setMerchantId(snapshot.getMerchantId());
            itemPo.setSkuName(snapshot.getSkuName());
            itemPo.setQuantity(quantity);
            itemPo.setUnitPrice(snapshot.getPrice());
            itemPo.setAmount(lineAmount);
            itemPo.setSubtotalAmount(lineAmount);
            itemPo.setDiscountAmount(BigDecimal.ZERO);
            itemPo.setPayAmount(lineAmount);
            itemPo.setRefundStatus(REFUND_STATUS_NONE);
            itemPo.setSkuSnapshotJson(toSnapshotJson(snapshot));
            itemPo.setSpuSnapshotJson(toSnapshotJson(java.util.Map.of("spuId", snapshot.getSpuId())));

            orderItems.add(itemPo);
        }

        BigDecimal orderAmount = calcAmount(orderItems);
        OrderPo po = new OrderPo();
        po.setOrderNo(orderNo);
        po.setOrderType(ORDER_TYPE_NORMAL);
        po.setUserId(userId);
        po.setMerchantId(merchantId);
        po.setAmount(orderAmount);
        po.setTotalAmount(orderAmount);
        po.setPayAmount(orderAmount);
        po.setFreightAmount(BigDecimal.ZERO);
        po.setDiscountAmount(BigDecimal.ZERO);
        po.setPayExpireAt(LocalDateTime.now().plusMinutes(PAY_EXPIRE_MINUTES));
        po.setStatus(OrderStatusEnum.PENDING.intCode());
        po.setOrderGroupNo(orderGroupNo);
        po.setReceiverSnapshotJson(toReceiverSnapshotJson(receiver));
        po.setIdempotencyKey(idempotencyKey);
        orderManager.save(po);

        for (OrderItemPo itemPo : orderItems) {
            itemPo.setOrderId(po.getId());
            orderItemManager.save(itemPo);
        }

        Long payId =
                orderRemoteGateway
                        .createPayment(
                                new PayCreateDto(String.valueOf(po.getId()), userId, merchantId, orderAmount, null))
                        .getData();
        if (payId == null || payId < 0) {
            throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
        }
        po.setPayId(payId);
        orderManager.updateById(po);

        Long cleanupTaskId = createCartCleanupTask(userId, orderGroupNo, cleanupItems);
        log.info(
                "订单结算完成，orderId={}，userId={}，merchantId={}，groupNo={}，items={}，amount={}",
                po.getId(),
                userId,
                merchantId,
                orderGroupNo,
                orderItems.size(),
                orderAmount);
        return new CheckoutResultDto(po.getId(), orderGroupNo, cleanupTaskId);
    }

    /**
     * 为购物车结算创建异步清理任务。
     *
     * <p>无清理条目时返回 null；否则保存包含用户、订单组号和条目 JSON 的 PENDING 任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param orderGroupNo 订单组号，用于关联一次结算产生的订单和购物车清理任务
     * @param cleanupItems 结算成功后待清理的购物车条目快照
     * @return 新任务 ID，或无需清理时的 null。
     * @throws BizException 任务保存失败。
     */
    private Long createCartCleanupTask(
            Long userId, String orderGroupNo, List<CartCheckoutItemDto> cleanupItems) {
        if (cleanupItems == null || cleanupItems.isEmpty()) {
            return null;
        }
        CartCleanupTaskPo task = new CartCleanupTaskPo();
        task.setOrderGroupNo(orderGroupNo);
        task.setUserId(userId);
        task.setItemsJson(toSnapshotJson(cleanupItems));
        task.setStatus(CartCleanupStatusEnum.PENDING.intCode());
        task.setRetryCount(0);
        task.setNextRetryAt(LocalDateTime.now());
        if (task.getItemsJson() == null || !cartCleanupTaskManager.save(task)) {
            throw new BizException(CommonCode.SYS_ERROR, "购物车清理任务创建失败");
        }
        return task.getId();
    }

    /**
     * 合并重复 SKU 并校验结算明细。
     *
     * <p>拒绝空列表、空 SKU、非正数量、整数溢出及超过 100 个唯一 SKU，保留首次出现顺序。
     *
     * @param items 订单 SKU 与数量明细
     * @return 以 SKU ID 为键的合并数量映射。
     * @throws BizException 明细非法或数量超限。
     */
    private Map<Long, Integer> mergeCheckoutItems(List<OrderCheckoutItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(OrderCodeEnum.ORDER_ITEMS_EMPTY);
        }
        Map<Long, Integer> quantityBySku = new LinkedHashMap<>(items.size() * 2);
        for (OrderCheckoutItemDto item : items) {
            if (item == null
                    || item.getSkuId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new BizException(OrderCodeEnum.ORDER_CHECKOUT_ITEM_INVALID);
            }
            long merged = (long) quantityBySku.getOrDefault(item.getSkuId(), 0) + item.getQuantity();
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
     * 批量加载并校验结算所需 SKU 快照。
     *
     * <p>商品服务返回的映射必须覆盖全部请求 SKU，否则拒绝继续结算。
     *
     * @param skuIds 待批量读取的 SKU ID 列表
     * @return 完整 SKU 快照映射。
     * @throws BizException 快照缺失。
     */
    private Map<Long, SkuSnapshotDto> loadCheckoutSnapshots(List<Long> skuIds) {
        Result<Map<Long, SkuSnapshotDto>> result = orderRemoteGateway.batchSkuSnapshot(skuIds);
        Map<Long, SkuSnapshotDto> snapshots = result == null ? null : result.getData();
        if (snapshots == null
                || snapshots.size() != skuIds.size()
                || !snapshots.keySet().containsAll(skuIds)) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        return snapshots;
    }

    /**
     * 以统一订单号批量预占结算库存。
     *
     * <p>任一 SKU 预占失败都会让结算停止，交由外层全局事务处理已产生的远端操作。
     *
     * @param quantityBySku 已完成去重合并的 SKU 到数量映射
     * @param orderNoBySku SKU 到统一结算订单号的映射
     * @throws BizException 库存服务失败或返回非成功码。
     */
    private void reserveCheckoutStock(
            Map<Long, Integer> quantityBySku, Map<Long, String> orderNoBySku) {
        List<StockReserveDto> reserveItems =
                quantityBySku.entrySet().stream()
                        .map(
                                entry ->
                                        new StockReserveDto(
                                                entry.getKey(), entry.getValue(), orderNoBySku.get(entry.getKey())))
                        .toList();
        Result<Boolean> result =
                orderRemoteGateway.reserveBatch(new StockBatchReserveDto(reserveItems));
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || !Boolean.TRUE.equals(result.getData())) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }
    }

    /**
     * 校验结算商品属于单一商家。
     *
     * <p>逐个快照检查价格与商家字段；所有 SKU 必须来自同一 merchantId。
     *
     * @param quantityBySku 已完成去重合并的 SKU 到数量映射
     * @param snapshots SKU ID 到商品快照的完整映射
     * @return 本次结算的唯一商家 ID。
     * @throws BizException 快照字段缺失或跨商家。
     */
    private Long resolveCheckoutMerchantId(
            Map<Long, Integer> quantityBySku, Map<Long, SkuSnapshotDto> snapshots) {
        Long checkoutMerchantId = null;
        for (Long skuId : quantityBySku.keySet()) {
            SkuSnapshotDto snapshot = snapshots.get(skuId);
            if (snapshot == null || snapshot.getPrice() == null || snapshot.getMerchantId() == null) {
                throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
            }
            if (checkoutMerchantId == null) {
                checkoutMerchantId = snapshot.getMerchantId();
            } else if (!checkoutMerchantId.equals(snapshot.getMerchantId())) {
                throw new BizException(OrderCodeEnum.ORDER_MULTI_MERCHANT_NOT_ALLOWED);
            }
        }
        return checkoutMerchantId;
    }

    /**
     * 把同一结算订单号映射到每个 SKU。
     *
     * <p>库存预占需要为批量明细保留统一业务幂等键。
     *
     * @param quantityBySku 已完成去重合并的 SKU 到数量映射
     * @param orderNo 业务订单号
     * @return SKU 到订单号的映射。
     */
    private Map<Long, String> mapCheckoutOrderNo(Map<Long, Integer> quantityBySku, String orderNo) {
        Map<Long, String> orderNoBySku = new LinkedHashMap<>(quantityBySku.size() * 2);
        for (Long skuId : quantityBySku.keySet()) {
            orderNoBySku.put(skuId, orderNo);
        }
        return orderNoBySku;
    }

    /**
     * 把待支付订单推进为已支付并写入支付成功事件。
     *
     * <p>重复支付请求对已支付订单直接返回；其他非待支付状态拒绝。状态更新和发件箱写入位于同一事务。
     *
     * @param orderId 订单 ID
     * @throws BizException 订单不存在或状态不允许支付。
     */
    public void pay(Long orderId) {
        doPay(getOrder(orderId));
    }

    /**
     * 执行已加载订单的支付状态推进。
     *
     * <p>已支付订单直接跳过；仅待支付订单可在同一事务内更新状态并写入 Outbox，其他状态拒绝。
     *
     * @param po 订单持久化对象
     * @throws BizException 状态非法或事务内状态更新失败。
     */
    private void doPay(OrderPo po) {
        Long orderId = po.getId();

        // 确保订单状态为已支付的不会被重复处理
        if (po.getStatus().equals(OrderStatusEnum.PAID.intCode())) {
            log.info("订单已支付，跳过重复推进，orderId={}", orderId);
            return;
        }

        // 确保订单状态为待支付
        if (!po.getStatus().equals(OrderStatusEnum.PENDING.intCode())) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }

        OrderPaidDto event =
                new OrderPaidDto(orderId, po.getUserId(), po.getAmount(), LocalDateTime.now().toString());
        transactionTemplate.executeWithoutResult(
                status -> {
                    transitStatus(orderId, OrderStatusEnum.PAID, "USER_PAY", null);
                    orderOutboxService.recordPaidEvent(event);
                });
        log.info("订单已支付，orderId={}", orderId);
    }

    /**
     * 按订单号定位订单后执行支付状态推进。
     *
     * <p>先按 orderNo 查询唯一订单，再复用 pay 的幂等和事务逻辑。
     *
     * @param orderNo 业务订单号
     * @throws BizException 订单号为空、订单不存在或状态不允许支付。
     */
    public void payByOrderNo(String orderNo) {
        OrderPo po =
                orderManager.getOne(
                        Wrappers.lambdaQuery(OrderPo.class).eq(OrderPo::getOrderNo, orderNo).last("LIMIT 1"));
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        pay(po.getId());
    }

    /**
     * 判断订单当前是否已经是已支付状态。
     *
     * <p>订单不存在或状态不是 PAID 时返回 false；该方法不抛业务异常。
     *
     * @param orderId 订单 ID
     * @return 订单存在且状态为 PAID 时返回 true。
     */
    private boolean isOrderPaid(Long orderId) {
        OrderPo po = orderManager.getById(orderId);
        return po != null && po.getStatus() == OrderStatusEnum.PAID.intCode();
    }

    /**
     * 由用户取消订单并释放已预占库存。
     *
     * <p>先校验订单归属和状态，再记录库存待释放状态、推进为已取消并调用库存服务；释放失败保留待补偿状态。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或当前状态不允许取消。
     */
    public void cancel(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
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
     * 由商家完成发货并写入物流节点。
     *
     * <p>校验订单归属商家、推进为已发货，并保存承运商和运单号组成的物流描述。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param carrier 承运商名称
     * @param trackingNo 物流运单号
     * @throws BizException 订单不存在、商家不匹配或订单状态不允许发货。
     */
    public void ship(Long orderId, Long merchantId, String carrier, String trackingNo) {
        OrderPo po = getOrder(orderId);
        if (po.getMerchantId() == null || !po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.SHIPPED, "MERCHANT_SHIP", null);
        OrderLogisticsPo node = new OrderLogisticsPo();
        node.setOrderId(orderId);
        node.setNode("已发货");
        node.setDescription(String.format("%s · %s", carrier, trackingNo));
        node.setCreatedTime(LocalDateTime.now());
        orderLogisticsManager.save(node);
        log.info(
                "订单已发货，orderId={}，merchantId={}，carrier={}，trackingNo={}",
                orderId,
                merchantId,
                carrier,
                trackingNo);
    }

    /**
     * 由商家发起指定金额的退款申请。
     *
     * <p>校验商家归属、当前购买态和退款金额，只登记退款中状态；默认退款请求号由订单 ID 派生。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 商家不匹配、状态不允许、金额非法或既有退款请求号冲突。
     */
    public void merchantInitiateRefund(
            Long orderId, Long merchantId, String refundAmount, String reason) {
        merchantInitiateRefund(orderId, merchantId, refundAmount, reason, "R" + orderId);
    }

    /**
     * 由商家发起指定金额的退款申请。
     *
     * <p>校验商家归属、当前购买态和退款金额，只登记退款中状态；默认退款请求号由订单 ID 派生。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 商家不匹配、状态不允许、金额非法或既有退款请求号冲突。
     */
    public void merchantInitiateRefund(
            Long orderId, Long merchantId, String refundAmount, String reason, String refundRequestNo) {
        OrderPo po = getOrder(orderId);
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
        log.info(
                "商家已发起退款，orderId={}，merchantId={}，amount={}，reason={}",
                orderId,
                merchantId,
                refundAmount,
                reason);
    }

    /**
     * 按退款单号解析订单后发起商家退款。
     *
     * <p>退款单号可带 R 前缀；解析得到的订单 ID 会继续走商家退款校验和状态推进。
     *
     * @param refundNo 退款单号，可带 R 前缀
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 退款单号为空、格式错误或退款申请非法。
     */
    public void merchantInitiateRefundByNo(
            String refundNo, Long merchantId, String refundAmount, String reason) {
        merchantInitiateRefund(
                parseOrderIdFromRefundNo(refundNo), merchantId, refundAmount, reason, refundNo);
    }

    /**
     * 由用户确认收货并把已发货订单推进为已完成。
     *
     * <p>校验订单归属后更新状态、收货时间并写入状态日志。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或状态不允许确认。
     */
    public void confirm(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.COMPLETED, "USER_CONFIRM", null);
        log.info("订单已确认收货，orderId={}，userId={}", orderId, userId);
    }

    /**
     * 登记用户退款申请并进入退款中状态。
     *
     * <p>只保存退款来源状态和稳定退款请求号，不直接调用支付网关；真实退款必须经过商家审核或平台仲裁。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或当前状态不允许申请退款。
     */
    public void refund(Long orderId, Long userId) {
        refund(orderId, userId, UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 登记用户退款申请并进入退款中状态。
     *
     * <p>只保存退款来源状态和稳定退款请求号，不直接调用支付网关；真实退款必须经过商家审核或平台仲裁。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 订单不存在、归属不匹配或当前状态不允许申请退款。
     */
    private void refund(Long orderId, Long userId, String refundRequestNo) {
        OrderPo po = getOrder(orderId);
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
     * 处理支付侧退款回调并推进订单退款结果。
     *
     * <p>校验支付单号、退款请求号和累计金额；重复累计金额直接忽略，全额退款进入已退款，部分退款回退到退款前状态并清理本轮请求键。
     *
     * @param dto 支付退款回调数据
     * @throws BizException 回调字段缺失、金额不一致、支付单或请求号冲突、状态非退款中。
     */
    public void handleRefundCallback(RefundCallbackDto dto) {
        if (dto == null
                || dto.getOrderId() == null
                || dto.getRefundedAmount() == null
                || dto.getTotalRefundedAmount() == null) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        OrderPo po = getOrder(dto.getOrderId());
        if (po.getPayId() != null && dto.getPayId() != null && !po.getPayId().equals(dto.getPayId())) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (po.getRefundRequestNo() != null
                && !po.getRefundRequestNo().isBlank()
                && dto.getRefundRequestNo() != null
                && !po.getRefundRequestNo().equals(dto.getRefundRequestNo())) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }

        BigDecimal refundedAmount = normalizeAmount(dto.getRefundedAmount());
        BigDecimal totalRefunded = normalizeAmount(dto.getTotalRefundedAmount());
        BigDecimal currentRefunded =
                po.getTotalRefundedAmount() == null ? BigDecimal.ZERO : po.getTotalRefundedAmount();
        if (totalRefunded.compareTo(currentRefunded) <= 0) {
            log.info("退款回调已忽略，累计退款金额未变化，orderId={}，total={}", dto.getOrderId(), currentRefunded);
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
        boolean fullyRefunded =
                Boolean.TRUE.equals(dto.getFullyRefunded()) || totalRefunded.compareTo(payable) >= 0;
        if (fullyRefunded) {
            transition(po, OrderStatusEnum.REFUNDED, "PAY_REFUND_CALLBACK", null, null);
            log.info("订单已全额退款，orderId={}，totalRefunded={}", po.getId(), totalRefunded);
        } else {
            // 部分退款回到退款前状态，并清理本轮退款请求键。
            po.setRefundAmount(null);
            po.setRefundRequestNo(null);
            transition(po, revertTarget(po), "PAY_REFUND_CALLBACK_PARTIAL", null, null);
            log.info("订单已部分退款，orderId={}，totalRefunded={}", po.getId(), totalRefunded);
        }
    }

    /**
     * 读取订单状态码。
     *
     * <p>带 userId 的重载校验订单归属，单参数版本供内部可信调用；不修改订单。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 订单状态整数编码。
     * @throws BizException 订单不存在或用户不匹配。
     */
    @Override
    public Integer getStatus(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po.getStatus();
    }

    /**
     * 读取订单状态码。
     *
     * <p>带 userId 的重载校验订单归属，单参数版本供内部可信调用；不修改订单。
     *
     * @param orderId 订单 ID
     * @return 订单状态整数编码。
     * @throws BizException 订单不存在或用户不匹配。
     */
    @Override
    public Integer getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
    }

    /**
     * 按订单号或数字订单 ID 解析订单归属商家。
     *
     * <p>先按 orderNo 精确查询，未命中且 ref 为纯数字时再按主键查询；不存在时返回 null。
     *
     * @param ref 订单号或数字订单 ID
     * @return 商家 ID，或订单不存在时的 null。
     */
    @Override
    public Long resolveMerchantId(String ref) {
        OrderPo po = findOrderByRef(ref);
        return po == null ? null : po.getMerchantId();
    }

    /**
     * 查询支付服务所需的订单支付快照。
     *
     * <p>按订单号或数字订单 ID 定位订单，返回支付金额、状态、用户、商家和支付流水号。
     *
     * @param ref 订单号或数字订单 ID
     * @return 订单支付快照。
     * @throws BizException 订单不存在。
     */
    @Override
    public OrderPaySnapshotDto getPaySnapshot(String ref) {
        OrderPo po = findOrderByRef(ref);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        BigDecimal payAmount = po.getPayAmount() != null ? po.getPayAmount() : po.getAmount();
        return new OrderPaySnapshotDto(
                po.getId(),
                po.getOrderNo(),
                po.getUserId(),
                po.getMerchantId(),
                payAmount,
                po.getStatus(),
                po.getPayId());
    }

    /**
     * 判断用户是否可就指定 SPU 评价订单。
     *
     * <p>要求订单归属用户、状态属于已支付/已发货/已完成且订单明细包含该 SPU；商家优先取明细快照。
     *
     * @param orderNo 业务订单号
     * @param userId 用户 ID，必须对应当前订单归属
     * @param spuId SPU ID
     * @return 包含资格、订单、用户、商家和订单状态的快照对象；非法输入返回不符合条件。
     */
    @Override
    public OrderCommentEligibilityDto commentEligibility(String orderNo, Long userId, Long spuId) {
        OrderCommentEligibilityDto notEligible =
                new OrderCommentEligibilityDto(false, null, userId, null, null);
        if (orderNo == null || orderNo.isBlank() || userId == null || spuId == null) {
            return notEligible;
        }
        OrderPo po = findOrderByRef(orderNo);
        if (po == null || !userId.equals(po.getUserId())) {
            return notEligible;
        }
        if (!PURCHASED_STATUS_CODES.contains(po.getStatus())) {
            return eligibilityOf(po, false, po.getMerchantId());
        }
        OrderItemPo matched =
                orderItemManager.getOne(
                        Wrappers.lambdaQuery(OrderItemPo.class)
                                .eq(OrderItemPo::getOrderId, po.getId())
                                .eq(OrderItemPo::getSpuId, spuId)
                                .last("LIMIT 1"));
        if (matched == null) {
            return eligibilityOf(po, false, po.getMerchantId());
        }
        Long merchantId =
                matched.getMerchantId() != null ? matched.getMerchantId() : po.getMerchantId();
        return eligibilityOf(po, true, merchantId);
    }

    /**
     * 构造评价资格结果快照。
     *
     * <p>把资格布尔值、订单、用户、商家和状态组装为跨服务 DTO，不修改数据库。
     *
     * @param po 订单持久化对象
     * @param eligible 评价资格判断结果
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 评价资格快照。
     */
    private OrderCommentEligibilityDto eligibilityOf(OrderPo po, boolean eligible, Long merchantId) {
        return new OrderCommentEligibilityDto(
                eligible, po.getId(), po.getUserId(), merchantId, po.getStatus());
    }

    /**
     * 按订单号或数字订单主键查询订单。
     *
     * <p>先按 orderNo 查询，未命中且 ref 为纯数字时再按 ID 查询；空值直接返回 null。
     *
     * @param ref 订单号或数字订单 ID
     * @return 命中的订单或 null。
     */
    private OrderPo findOrderByRef(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        OrderPo po =
                orderManager.getOne(
                        Wrappers.lambdaQuery(OrderPo.class).eq(OrderPo::getOrderNo, ref).last("LIMIT 1"));
        if (po != null) {
            return po;
        }
        if (ref.matches("\\d+")) {
            try {
                po = orderManager.getById(Long.valueOf(ref));
            } catch (NumberFormatException e) {
                po = null;
            }
        }
        return po;
    }

    /**
     * 查询订单详情并可按用户身份校验归属。
     *
     * <p>带 userId 的重载在订单不存在或归属不匹配时按订单不存在处理；单参数版本供可信内部链路使用。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 订单持久化对象。
     * @throws BizException 订单不存在或用户不匹配。
     */
    public OrderPo getDetail(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }

    /**
     * 查询订单详情并可按用户身份校验归属。
     *
     * <p>带 userId 的重载在订单不存在或归属不匹配时按订单不存在处理；单参数版本供可信内部链路使用。
     *
     * @param orderId 订单 ID
     * @return 订单持久化对象。
     * @throws BizException 订单不存在或用户不匹配。
     */
    public OrderPo getDetail(Long orderId) {
        return getOrder(orderId);
    }

    /**
     * 查询订单明细并组装展示对象。
     *
     * <p>批量解析 SKU 图片公开地址并补充 SPU 名称；订单无明细时返回空列表。
     *
     * @param orderId 订单 ID
     * @return 订单明细视图列表。
     */
    @Override
    public List<OrderItemVo> listItemVo(Long orderId) {
        List<OrderItemPo> items =
                orderItemManager.list(
                        Wrappers.lambdaQuery(OrderItemPo.class).eq(OrderItemPo::getOrderId, orderId));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrderItemVo> vos =
                items.stream()
                        .map(po -> OrderItemVo.from(po, minioUtil))
                        .collect(java.util.stream.Collectors.toList());
        fillSpuName(vos);
        return vos;
    }

    /**
     * 批量补充订单明细的 SPU 标题。
     *
     * <p>对去重后的 SPU 调用商品服务，失败或无快照时保留原值，不阻断明细返回。
     *
     * @param vos 待补全的订单或明细视图列表
     */
    private void fillSpuName(List<OrderItemVo> vos) {
        List<Long> spuIds =
                vos.stream()
                        .map(OrderItemVo::getSpuId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
        if (spuIds.isEmpty()) {
            return;
        }
        Result<java.util.Map<Long, SpuSnapshotDto>> result =
                orderRemoteGateway.batchSpuSnapshot(spuIds);
        java.util.Map<Long, SpuSnapshotDto> snapshots =
                (result != null && result.getData() != null) ? result.getData() : Collections.emptyMap();
        for (OrderItemVo vo : vos) {
            SpuSnapshotDto snap = snapshots.get(vo.getSpuId());
            if (snap != null) {
                vo.setSpuName(snap.getTitle());
            }
        }
    }

    /**
     * 由商家关闭订单。
     *
     * <p>商家身份来自请求头，关闭和库存释放规则由 OrderService 执行。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 空数据的统一响应。
     * @throws BizException 商家不匹配或订单状态不允许关闭。
     */
    public void merchantClose(Long orderId, Long merchantId) {
        OrderPo po = getOrder(orderId);
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
     * 删除满足终态的订单记录。
     *
     * <p>只允许用户删除自己的已取消、已完成或已退款订单，执行逻辑删除；其他状态拒绝。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或状态不允许删除。
     */
    public void delete(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
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
     * 由平台关闭订单并触发库存释放。
     *
     * <p>推进订单为已取消，记录平台关闭原因并尝试释放库存；释放失败保留待补偿状态。
     *
     * @param orderId 订单 ID
     * @throws BizException 订单不存在或状态不允许平台关闭。
     */
    public void platformClose(Long orderId) {
        OrderPo po = getOrder(orderId);
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
     * 查询商家待办订单数量。
     *
     * <p>统计待支付、已支付和退款中状态，不包含终态订单。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单数量统一响应。
     */
    @Override
    public long pendingCount(Long merchantId) {
        return orderManager.count(
                Wrappers.lambdaQuery(OrderPo.class)
                        .eq(OrderPo::getMerchantId, merchantId)
                        .in(
                                OrderPo::getStatus,
                                List.of(
                                        OrderStatusEnum.PENDING.intCode(),
                                        OrderStatusEnum.PAID.intCode(),
                                        OrderStatusEnum.REFUNDING.intCode())));
    }

    /**
     * 按主键读取订单，不存在时终止当前业务。
     *
     * <p>订单为空时统一转换为 ORDER_NOT_FOUND 业务异常。
     *
     * @param orderId 订单 ID
     * @return 存在的订单持久化对象。
     * @throws BizException 订单不存在。
     */
    private OrderPo getOrder(Long orderId) {
        OrderPo po = orderManager.getById(orderId);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }

    /**
     * 释放订单占用的库存。
     *
     * <p>优先按订单明细逐项释放并保留订单号幂等键；无明细时回退到订单主记录的 SKU 和数量；任一调用失败返回 false。
     *
     * @param po 订单持久化对象
     * @return 全部释放成功或无需释放时返回 true。
     */
    private boolean releaseStockForOrder(OrderPo po) {
        List<OrderItemPo> items =
                orderItemManager.list(
                        Wrappers.lambdaQuery(OrderItemPo.class).eq(OrderItemPo::getOrderId, po.getId()));
        if (!items.isEmpty()) {
            for (OrderItemPo item : items) {
                Result<Boolean> result =
                        orderRemoteGateway.release(
                                new StockReserveDto(item.getSkuId(), item.getQuantity(), po.getOrderNo()));
                if (result == null
                        || !CommonCode.SUCCESS.equals(result.getCode())
                        || !Boolean.TRUE.equals(result.getData())) {
                    return false;
                }
            }
            return true;
        }
        if (po.getSkuId() != null && po.getQuantity() != null) {
            Result<Boolean> result =
                    orderRemoteGateway.release(
                            new StockReserveDto(po.getSkuId(), po.getQuantity(), po.getOrderNo()));
            return result != null
                    && CommonCode.SUCCESS.equals(result.getCode())
                    && Boolean.TRUE.equals(result.getData());
        }
        return true;
    }

    /**
     * 持久化库存释放补偿状态。
     *
     * <p>成功写 RELEASED，失败写 PENDING，供后续扫描继续重试。
     *
     * @param po 订单持久化对象
     * @param released 本次库存释放是否成功
     */
    private void markStockReleaseResult(OrderPo po, boolean released) {
        po.setStockReleaseStatus(
                (released ? StockReleaseStatusEnum.RELEASED : StockReleaseStatusEnum.PENDING).intCode());
        orderManager.updateById(po);
    }

    /**
     * 读取订单后执行状态迁移。
     *
     * <p>把订单 ID 解析为持久化对象，再复用统一 transition 事务逻辑。
     *
     * @param orderId 订单 ID
     * @param target 目标订单状态
     * @param triggerSource 状态迁移触发来源
     * @param closeReason 关闭原因，写入订单关闭字段
     * @throws BizException 订单不存在、状态非法或条件更新失败。
     */
    private void transitStatus(
            Long orderId, OrderStatusEnum target, String triggerSource, String closeReason) {
        transition(getOrder(orderId), target, triggerSource, closeReason, null);
    }

    /**
     * 在本地事务内执行订单状态迁移和状态日志写入。
     *
     * <p>记录迁移前状态与版本，校验状态机，应用目标时间戳，并通过条件更新结果检测并发冲突。
     *
     * @param po 订单持久化对象
     * @param target 目标订单状态
     * @param triggerSource 状态迁移触发来源
     * @param closeReason 关闭原因，写入订单关闭字段
     * @param note 状态日志说明
     * @throws BizException 状态非法或更新 0 行。
     */
    private void transition(
            OrderPo po, OrderStatusEnum target, String triggerSource, String closeReason, String note) {
        // 使用编程式事务，确保状态转换的原子性
        transactionTemplate.executeWithoutResult(
                status -> {
                    final Integer fromStatus = po.getStatus();
                    final Integer fromVersion = po.getVersion();
                    statusMachine.transit(po, target);
                    applyTargetTimestamps(po, target, closeReason);
                    if (!orderManager.updateById(po)) {
                        // 并发下后写者更新 0 行：目标状态已被先行事务推进，拒绝继续执行副作用
                        throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
                    }
                    saveStatusLog(
                            po,
                            fromStatus,
                            target,
                            fromVersion == null ? null : fromVersion + 1,
                            triggerSource,
                            note);
                });
    }

    /**
     * 计算退款回退目标状态。
     *
     * <p>优先使用退款来源状态，缺失时降级为已支付。
     *
     * @param po 订单持久化对象
     * @return 退款前的订单状态。
     */
    private OrderStatusEnum revertTarget(OrderPo po) {
        Integer fromStatus = po.getRefundFromStatus();
        return fromStatus != null ? statusMachine.fromCode(fromStatus) : OrderStatusEnum.PAID;
    }

    /**
     * 按目标状态补充订单业务时间戳。
     *
     * <p>支付、发货、完成、取消和退款分别写入对应时间；取消同时保存关闭原因。
     *
     * @param po 订单持久化对象
     * @param target 目标订单状态
     * @param closeReason 关闭原因，写入订单关闭字段
     */
    private void applyTargetTimestamps(OrderPo po, OrderStatusEnum target, String closeReason) {
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
            default -> {}
        }
    }

    /**
     * 保存订单状态迁移审计记录。
     *
     * <p>记录前后状态、迁移后版本、触发来源和说明，版本字段用于幂等去重。
     *
     * @param po 订单持久化对象
     * @param fromStatus 迁移前订单状态编码
     * @param toStatus 迁移后的订单状态
     * @param orderVersion 迁移后的订单版本，用于状态日志幂等
     * @param triggerSource 状态迁移触发来源
     * @param note 状态日志说明
     */
    private void saveStatusLog(
            OrderPo po,
            Integer fromStatus,
            OrderStatusEnum toStatus,
            Integer orderVersion,
            String triggerSource,
            String note) {
        OrderStatusLogPo log = new OrderStatusLogPo();
        log.setOrderId(po.getId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus.intCode());
        log.setOrderVersion(orderVersion);
        log.setTriggerSource(triggerSource);
        log.setNote(note);
        orderStatusLogManager.save(log);
    }

    /**
     * 通过 ID 服务生成带 OD 前缀的订单号。
     *
     * <p>全局 ID 获取失败时由 requireGlobalId 转成订单业务异常。
     *
     * @return 最终订单号。
     * @throws BizException 全局 ID 获取失败。
     */
    private String generateOrderNo() {
        return "OD" + requireGlobalId("ORDER");
    }

    /**
     * 通过 ID 服务生成带 OG 前缀的订单组号。
     *
     * <p>订单组号用于关联结算产生的订单及购物车清理任务。
     *
     * @return 最终订单组号。
     * @throws BizException 全局 ID 获取失败。
     */
    private String generateOrderGroupNo() {
        return "OG" + requireGlobalId("ORDER_GROUP");
    }

    /**
     * 获取并校验业务号段 ID。
     *
     * <p>ID 服务返回空、失败码或异常时统一抛出 ORDER_ID_GENERATE_FAIL。
     *
     * @param bizTag ID 服务的业务号段标识
     * @return 有效的全局 ID。
     * @throws BizException ID 服务不可用或返回无效值。
     */
    private Long requireGlobalId(String bizTag) {
        try {
            Result<Long> result = orderRemoteGateway.nextId(bizTag);
            if (result != null
                    && CommonCode.SUCCESS.equals(result.getCode())
                    && result.getData() != null) {
                return result.getData();
            }
            log.error("id-service 返回失败，bizTag={}，result={}", bizTag, result);
        } catch (Exception e) {
            log.error("id-service nextId 失败，bizTag={}", bizTag, e);
        }
        throw new BizException(OrderCodeEnum.ORDER_ID_GENERATE_FAIL);
    }

    /**
     * 校验并规范化退款金额。
     *
     * <p>金额必须大于 0、不超过剩余可退金额，且最多两位小数；超出精度的尾数不会静默舍入。
     *
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param po 订单持久化对象
     * @return 规范到两位小数的退款金额。
     * @throws BizException 金额为空、格式错误、非正、超额或精度非法。
     */
    private BigDecimal validateRefundAmount(String refundAmount, OrderPo po) {
        BigDecimal refundAmt;
        try {
            refundAmt = new BigDecimal(refundAmount);
        } catch (NumberFormatException | NullPointerException e) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        BigDecimal payAmount = po.getPayAmount() != null ? po.getPayAmount() : po.getAmount();
        BigDecimal totalRefunded =
                po.getTotalRefundedAmount() == null ? BigDecimal.ZERO : po.getTotalRefundedAmount();
        BigDecimal remainingAmount = payAmount == null ? null : payAmount.subtract(totalRefunded);
        if (payAmount == null
                || refundAmt.compareTo(BigDecimal.ZERO) <= 0
                || remainingAmount.compareTo(BigDecimal.ZERO) <= 0
                || refundAmt.compareTo(remainingAmount) > 0) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        try {
            return refundAmt.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
    }

    /**
     * 把订单快照对象序列化为 JSON。
     *
     * <p>序列化失败时记录告警并返回 null，由调用方决定是否接受该快照。
     *
     * @param value 待序列化的快照对象
     * @return JSON 文本或 null。
     */
    private String toSnapshotJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化订单快照失败", e);
            return null;
        }
    }

    /**
     * 把收货信息转换为订单快照 JSON。
     *
     * <p>收货信息为空时返回 null，否则复用通用 JSON 序列化逻辑。
     *
     * @param receiver 收货信息快照
     * @return 收货快照 JSON 或 null。
     */
    private String toReceiverSnapshotJson(ReceiverDto receiver) {
        return receiver == null ? null : toSnapshotJson(receiver);
    }

    /**
     * 汇总订单明细金额并规范精度。
     *
     * <p>逐项累加 BigDecimal 金额，最终按两位小数校验。
     *
     * @param itemPos 订单明细持久化对象列表
     * @return 订单总金额。
     * @throws BizException 累加结果精度超过两位小数。
     */
    private BigDecimal calcAmount(List<OrderItemPo> itemPos) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemPo item : itemPos) {
            total = total.add(item.getAmount());
        }
        return normalizeAmount(total);
    }

    /**
     * 把订单金额严格规范为两位小数。
     *
     * <p>使用 RoundingMode.UNNECESSARY，任何多于两位的有效尾数都拒绝，不做静默进位。
     *
     * @param amount 订单金额，必须精确到两位小数
     * @return 两位小数金额。
     * @throws BizException 金额精度非法。
     */
    private static BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BizException(OrderCodeEnum.ORDER_AMOUNT_SCALE_INVALID);
        }
    }

    /**
     * 查询订单游标分页。
     *
     * <p>强制使用请求头身份作为筛选条件，并将分页参数转交订单服务。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单视图游标页。
     */
    public CursorPageVo<OrderPo> page(OrderPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        Integer statusCode = resolveStatusCode(dto.getStatus());
        List<OrderPo> records =
                orderManager.list(
                        Wrappers.lambdaQuery(OrderPo.class)
                                .lt(lastId != null, OrderPo::getId, lastId)
                                .eq(dto.getUserId() != null, OrderPo::getUserId, dto.getUserId())
                                .eq(dto.getMerchantId() != null, OrderPo::getMerchantId, dto.getMerchantId())
                                .eq(statusCode != null, OrderPo::getStatus, statusCode)
                                .ge(dto.getStartTime() != null, OrderPo::getCreateTime, dto.getStartTime())
                                .le(dto.getEndTime() != null, OrderPo::getCreateTime, dto.getEndTime())
                                .orderByDesc(OrderPo::getId)
                                .last("LIMIT " + (limit + 1)));
        return CursorPageVo.of(records, limit, OrderPo::getId);
    }

    /**
     * 查询订单游标页并转换为展示对象。
     *
     * <p>复用 page 的筛选和游标语义，不额外补充明细。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单视图游标页。
     */
    @Override
    public CursorPageVo<OrderVo> pageVo(OrderPageDto dto) {
        return page(dto).map(OrderVo::from);
    }

    /**
     * 查询用户订单详情并补充明细。
     *
     * <p>先校验订单归属，再转换主记录并加载订单明细视图。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 包含明细的订单视图。
     * @throws BizException 订单不存在或用户不匹配。
     */
    @Override
    public OrderVo detailVo(Long orderId, Long userId) {
        OrderVo vo = OrderVo.from(getDetail(orderId, userId));
        vo.setItems(listItemVo(orderId));
        return vo;
    }

    /**
     * 查询平台订单详情。
     *
     * <p>按订单 ID 读取主记录并转换为视图，不额外加载明细列表。
     *
     * @param orderId 订单 ID
     * @return 订单视图。
     * @throws BizException 订单不存在。
     */
    @Override
    public OrderVo platformDetailVo(Long orderId) {
        return OrderVo.from(getDetail(orderId));
    }

    /**
     * 查询商家订单游标页并补全列表展示字段。
     *
     * <p>批量填充明细汇总数量、SKU/SPU 展示名称和最近一次退款拒绝原因。
     *
     * @param dto 订单分页和筛选参数
     * @return 补全后的商家订单游标页。
     */
    @Override
    public CursorPageVo<OrderVo> merchantPageVo(OrderPageDto dto) {
        CursorPageVo<OrderVo> result = page(dto).map(OrderVo::from);
        fillItemSummary(result.getRecords());
        fillSkuName(result.getRecords());
        fillRejectReason(result.getRecords());
        return result;
    }

    /**
     * 为商家订单视图填充明细汇总展示。
     *
     * <p>按订单分组明细，计算总数量并生成最多两项的 SPU/SKU 摘要，超过两项时追加省略号。
     *
     * @param vos 待补全的订单或明细视图列表
     */
    private void fillItemSummary(List<OrderVo> vos) {
        List<Long> orderIds = vos.stream().map(OrderVo::getId).toList();
        List<OrderItemPo> items =
                orderItemManager.list(
                        Wrappers.lambdaQuery(OrderItemPo.class).in(OrderItemPo::getOrderId, orderIds));
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, List<OrderItemPo>> byOrder =
                items.stream().collect(Collectors.groupingBy(OrderItemPo::getOrderId));
        Map<Long, String> spuTitles = fetchSpuTitles(byOrder);
        for (OrderVo vo : vos) {
            List<OrderItemPo> orderItems = byOrder.getOrDefault(vo.getId(), Collections.emptyList());
            if (orderItems.isEmpty()) {
                continue;
            }
            if (vo.getQuantity() == null) {
                vo.setQuantity(
                        orderItems.stream()
                                .map(OrderItemPo::getQuantity)
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum());
            }
            vo.setSkuName(buildItemDisplay(orderItems, spuTitles));
        }
    }

    /**
     * 批量获取订单明细需要的 SPU 标题。
     *
     * <p>远程调用失败时返回空映射，由调用方降级为 SKU 名称展示。
     *
     * @param byOrder 按订单 ID 分组的明细列表
     * @return SPU ID 到标题的映射。
     */
    private Map<Long, String> fetchSpuTitles(Map<Long, List<OrderItemPo>> byOrder) {
        Set<Long> spuIds =
                byOrder.values().stream()
                        .flatMap(List::stream)
                        .map(OrderItemPo::getSpuId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (spuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Result<java.util.Map<Long, SpuSnapshotDto>> result =
                orderRemoteGateway.batchSpuSnapshot(new ArrayList<>(spuIds));
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        java.util.Map<Long, String> titles = new HashMap<>();
        result
                .getData()
                .forEach(
                        (spuId, snap) -> {
                            if (snap != null && snap.getTitle() != null) {
                                titles.put(spuId, snap.getTitle());
                            }
                        });
        return titles;
    }

    /**
     * 生成订单列表中的多商品摘要。
     *
     * <p>最多展示前两项，格式为 SPU 标题和 SKU 名称/数量组合，超过两项追加省略号。
     *
     * @param orderItems 订单明细列表
     * @param spuTitles SPU ID 到标题的映射
     * @return 拼接后的商品摘要。
     */
    private String buildItemDisplay(List<OrderItemPo> orderItems, Map<Long, String> spuTitles) {
        List<OrderItemPo> head = orderItems;
        boolean truncated = false;
        if (orderItems.size() > 2) {
            head = orderItems.subList(0, 2);
            truncated = true;
        }
        String joined =
                head.stream()
                        .map(item -> buildItemSegment(item, spuTitles))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("、"));
        return truncated ? joined + "…" : joined;
    }

    /**
     * 生成单个订单明细的展示片段。
     *
     * <p>有 SPU 标题时与 SKU 名称和数量组合，缺少标题时退化为 SKU 描述。
     *
     * @param item 订单明细持久化对象
     * @param spuTitles SPU ID 到标题的映射
     * @return 单条明细展示文本。
     */
    private String buildItemSegment(OrderItemPo item, Map<Long, String> spuTitles) {
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
     * 查询指定商家的订单详情并补全展示字段。
     *
     * <p>订单必须同时匹配订单 ID 和商家 ID；随后填充明细汇总、SKU 名称和退款拒绝原因。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 补全后的订单视图。
     * @throws BizException 订单不存在或不属于该商家。
     */
    @Override
    public OrderVo merchantDetailVo(Long orderId, Long merchantId) {
        OrderPo po =
                orderManager.getOne(
                        Wrappers.lambdaQuery(OrderPo.class)
                                .eq(OrderPo::getId, orderId)
                                .eq(OrderPo::getMerchantId, merchantId));
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        OrderVo vo = OrderVo.from(po);
        fillItemSummary(List.of(vo));
        fillSkuName(List.of(vo));
        fillRejectReason(List.of(vo));
        return vo;
    }

    /**
     * 为缺少 SKU 名称的订单视图补充快照名称。
     *
     * <p>批量请求商品服务，已有名称的视图不覆盖，远端失败时保留空值。
     *
     * @param vos 待补全的订单或明细视图列表
     */
    private void fillSkuName(List<OrderVo> vos) {
        List<Long> skuIds =
                vos.stream().map(OrderVo::getSkuId).filter(Objects::nonNull).distinct().toList();
        if (skuIds.isEmpty()) {
            return;
        }
        Result<Map<Long, SkuSnapshotDto>> result = orderRemoteGateway.batchSkuSnapshot(skuIds);
        Map<Long, SkuSnapshotDto> snapshots =
                (result != null && result.getData() != null) ? result.getData() : Collections.emptyMap();
        for (OrderVo vo : vos) {
            if (vo.getSkuName() != null) {
                continue;
            }
            SkuSnapshotDto snap = snapshots.get(vo.getSkuId());
            if (snap != null) {
                vo.setSkuName(snap.getSkuName());
            }
        }
    }

    /**
     * 补充商家退款拒绝原因。
     *
     * <p>查询 MERCHANT_REJECT_REFUND 状态日志，每个订单取最新一条非空 note。
     *
     * @param vos 待补全的订单或明细视图列表
     */
    private void fillRejectReason(List<OrderVo> vos) {
        List<Long> orderIds = vos.stream().map(OrderVo::getId).toList();
        if (orderIds.isEmpty()) {
            return;
        }
        List<OrderStatusLogPo> logs =
                orderStatusLogManager.list(
                        Wrappers.lambdaQuery(OrderStatusLogPo.class)
                                .in(OrderStatusLogPo::getOrderId, orderIds)
                                .eq(OrderStatusLogPo::getTriggerSource, "MERCHANT_REJECT_REFUND")
                                .isNotNull(OrderStatusLogPo::getNote)
                                .orderByDesc(OrderStatusLogPo::getId));
        Map<Long, String> reasonByOrder = new HashMap<>();
        for (OrderStatusLogPo log : logs) {
            reasonByOrder.putIfAbsent(log.getOrderId(), log.getNote());
        }
        for (OrderVo vo : vos) {
            vo.setRejectReason(reasonByOrder.get(vo.getId()));
        }
    }

    /**
     * 按商家和筛选条件查询 CSV 导出订单。
     *
     * <p>结果按创建时间倒序并受 CsvExporter 最大导出行数限制。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单主记录列表。
     */
    public List<OrderPo> exportList(Long merchantId, OrderPageDto dto) {
        Integer statusCode = resolveStatusCode(dto.getStatus());
        return orderManager.list(
                Wrappers.lambdaQuery(OrderPo.class)
                        .eq(OrderPo::getMerchantId, merchantId)
                        .eq(dto.getUserId() != null, OrderPo::getUserId, dto.getUserId())
                        .eq(statusCode != null, OrderPo::getStatus, statusCode)
                        .ge(dto.getStartTime() != null, OrderPo::getCreateTime, dto.getStartTime())
                        .le(dto.getEndTime() != null, OrderPo::getCreateTime, dto.getEndTime())
                        .orderByDesc(OrderPo::getCreateTime)
                        .last("LIMIT " + CsvExporter.maxExportRows()));
    }

    /**
     * 按商家筛选并转换为 CSV 导出视图。
     *
     * <p>复用 exportList 的查询和行数限制。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单视图列表。
     */
    @Override
    public List<OrderExportVo> exportVoList(Long merchantId, OrderPageDto dto) {
        return exportList(merchantId, dto).stream().map(OrderExportVo::from).toList();
    }

    /**
     * 按平台筛选条件查询 CSV 导出订单。
     *
     * <p>可叠加用户、商家、状态和时间范围，结果按创建时间倒序并限制最大行数。
     *
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单主记录列表。
     */
    public List<OrderPo> platformExportList(OrderPageDto dto) {
        Integer statusCode = resolveStatusCode(dto.getStatus());
        return orderManager.list(
                Wrappers.lambdaQuery(OrderPo.class)
                        .eq(dto.getUserId() != null, OrderPo::getUserId, dto.getUserId())
                        .eq(dto.getMerchantId() != null, OrderPo::getMerchantId, dto.getMerchantId())
                        .eq(statusCode != null, OrderPo::getStatus, statusCode)
                        .ge(dto.getStartTime() != null, OrderPo::getCreateTime, dto.getStartTime())
                        .le(dto.getEndTime() != null, OrderPo::getCreateTime, dto.getEndTime())
                        .orderByDesc(OrderPo::getCreateTime)
                        .last("LIMIT " + CsvExporter.maxExportRows()));
    }

    /**
     * 按平台筛选条件转换 CSV 导出视图。
     *
     * <p>接受分页 DTO 或起止日期字符串；日期参数非法时由解析方法降级为无边界。
     *
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单视图列表。
     */
    @Override
    public List<OrderExportVo> platformExportVoList(OrderPageDto dto) {
        return platformExportList(dto).stream().map(OrderExportVo::from).toList();
    }

    /**
     * 按平台筛选条件转换 CSV 导出视图。
     *
     * <p>接受分页 DTO 或起止日期字符串；日期参数非法时由解析方法降级为无边界。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @return 待导出的订单视图列表。
     */
    @Override
    public List<OrderExportVo> platformExportVoList(String startDate, String endDate) {
        OrderPageDto dto = new OrderPageDto();
        dto.setStartTime(parseStart(startDate));
        dto.setEndTime(parseEnd(endDate));
        return platformExportVoList(dto);
    }

    /**
     * 把状态 alias 解析为订单状态整数编码。
     *
     * <p>忽略空值并按 alias 不区分大小写匹配；无法识别时返回 null 表示不加状态条件。
     *
     * @param status 订单状态筛选值或状态编码
     * @return 状态整数编码或 null。
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
     * 执行订单统计 SQL。
     *
     * <p>按商家和时间范围聚合单量、支付金额、退款金额及分状态计数。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单统计数据库投影。
     */
    @Override
    public OrderStatisticsVo statistics(OrderPageDto dto) {
        OrderStatisticsResultDto result =
                orderManager.statistics(dto.getMerchantId(), dto.getStartTime(), dto.getEndTime());
        if (result == null) {
            return null;
        }
        return new OrderStatisticsVo(
                result.getTotalOrderCount(),
                result.getTotalAmount(),
                result.getRefundAmount(),
                result.getPendingCount(),
                result.getPaidCount(),
                result.getShippedCount(),
                result.getReceivedCount(),
                result.getCancelledCount(),
                result.getRefundingCount(),
                result.getRefundedCount());
    }

    /**
     * 汇总指定日期和商家的订单经营指标。
     *
     * <p>日期参数非法时忽略对应边界，merchantId 非法时按全平台汇总。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单汇总视图或 null。
     */
    @Override
    public OrderSummaryVo orderSummary(String startDate, String endDate, String merchantId) {
        OrderSummaryResultDto result =
                orderManager.summary(parseMerchantId(merchantId), parseStart(startDate), parseEnd(endDate));
        if (result == null) {
            return null;
        }
        return new OrderSummaryVo(
                result.getTotalCount(),
                result.getTotalAmount(),
                result.getPaidAmount(),
                result.getPendingAmount(),
                result.getRefundAmount(),
                result.getNetIncome());
    }

    /**
     * 按日期汇总订单单量和金额趋势。
     *
     * <p>日期或商家参数非法时降级为对应维度无筛选，无数据时返回空点位列表。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单趋势视图。
     */
    @Override
    public OrderTrendVo orderTrend(String startDate, String endDate, String merchantId) {
        List<OrderTrendPointDto> points =
                orderManager.trend(parseMerchantId(merchantId), parseStart(startDate), parseEnd(endDate));
        List<OrderTrendPointVo> vos =
                points == null
                        ? Collections.emptyList()
                        : points.stream()
                                .map(p -> new OrderTrendPointVo(p.getDate(), p.getCount(), p.getAmount()))
                                .toList();
        return new OrderTrendVo(vos);
    }

    /**
     * 解析可选的商家 ID。
     *
     * <p>空值返回 null；非数字文本记录告警并按全平台处理。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 商家 ID 或 null。
     */
    private Long parseMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(merchantId.trim());
        } catch (NumberFormatException e) {
            log.warn("订单汇总中的 merchantId 非法，按全平台处理。merchantId={}", merchantId);
            return null;
        }
    }

    /**
     * 把可选开始日期解析为当天零点。
     *
     * <p>空值或非法日期返回 null，效果是不设置开始时间下界。
     *
     * @param date yyyy-MM-dd 格式日期文本
     * @return 开始时间或 null。
     */
    private LocalDateTime parseStart(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (Exception e) {
            log.warn("订单汇总中的 startDate 非法，忽略边界。date={}", date);
            return null;
        }
    }

    /**
     * 把可选结束日期解析为当天最后一刻。
     *
     * <p>空值或非法日期返回 null，效果是不设置结束时间上界。
     *
     * @param date yyyy-MM-dd 格式日期文本
     * @return 结束时间或 null。
     */
    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atTime(LocalTime.MAX);
        } catch (Exception e) {
            log.warn("订单汇总中的 endDate 非法，忽略边界。date={}", date);
            return null;
        }
    }

    /**
     * 把用户订单状态分组计数转换为展示对象。
     *
     * <p>只映射 1 到 7 的已定义状态，缺失状态保持默认 0。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 用户订单状态计数视图。
     */
    @Override
    public OrderStatusCountsVo countStatusByUser(Long userId) {
        List<OrderStatusCountDto> rows = orderManager.countByStatus(userId);
        OrderStatusCountsVo vo = new OrderStatusCountsVo();
        for (OrderStatusCountDto row : rows) {
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
                default -> {}
            }
        }
        return vo;
    }

    /**
     * 审核退款中的商家订单。
     *
     * <p>驳回时回退到退款前状态并写入原因；通过时调用支付退款，订单状态由支付回调最终推进。退款请求号必须稳定一致。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 商家不匹配、订单非退款中、退款请求号冲突或支付退款失败。
     */
    public void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason) {
        reviewRefund(orderId, approved, merchantId, reason, null);
    }

    /**
     * 审核退款中的商家订单。
     *
     * <p>驳回时回退到退款前状态并写入原因；通过时调用支付退款，订单状态由支付回调最终推进。退款请求号必须稳定一致。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 商家不匹配、订单非退款中、退款请求号冲突或支付退款失败。
     */
    public void reviewRefund(
            Long orderId, boolean approved, Long merchantId, String reason, String refundRequestNo) {
        OrderPo po = getOrder(orderId);
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
     * 按退款单号审核商家退款。
     *
     * <p>从 R 前缀或纯数字退款单号解析订单 ID，再复用退款审核逻辑。
     *
     * @param refundNo 退款单号，可带 R 前缀
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 退款单号非法、商家不匹配或退款状态不允许审核。
     */
    public void reviewRefundByNo(String refundNo, boolean approved, Long merchantId, String reason) {
        reviewRefund(parseOrderIdFromRefundNo(refundNo), approved, merchantId, reason, refundNo);
    }

    /**
     * 从退款单号解析订单 ID。
     *
     * <p>允许 R 前缀或纯数字；R 前缀只移除一次，剩余部分必须是合法 Long。
     *
     * @param refundNo 退款单号，可带 R 前缀
     * @return 退款单号对应的订单 ID。
     * @throws BizException 退款单号为空或格式非法。
     */
    private Long parseOrderIdFromRefundNo(String refundNo) {
        if (refundNo == null || refundNo.isEmpty()) {
            throw new BizException(CommonCode.PARAM_INVALID, "退款单号不能为空");
        }
        String tail = refundNo.startsWith("R") ? refundNo.substring(1) : refundNo;
        try {
            return Long.parseLong(tail);
        } catch (NumberFormatException e) {
            throw new BizException(CommonCode.PARAM_INVALID, "退款单号格式错误：" + refundNo);
        }
    }

    /**
     * 由平台仲裁退款申请。
     *
     * <p>同意时直接提交支付退款并保持退款中等待回调；拒绝时回退到退款前状态。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @throws BizException 订单非退款中或支付退款失败。
     */
    public void arbitrateRefund(Long orderId, boolean approved) {
        OrderPo po = getOrder(orderId);
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
     * 提交订单支付退款请求。
     *
     * <p>退款金额优先使用订单记录的本次退款金额，否则使用订单金额；支付服务返回失败时抛出业务异常并保持退款中。
     *
     * @param po 订单持久化对象
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 支付退款失败。
     */
    private void requestPayRefund(OrderPo po, String refundRequestNo) {
        BigDecimal refundAmount = po.getRefundAmount() != null ? po.getRefundAmount() : po.getAmount();
        if (refundAmount == null) {
            throw new BizException(OrderCodeEnum.REFUND_AMOUNT_INVALID);
        }
        BigDecimal refundAmt = normalizeAmount(refundAmount);
        Result<Boolean> refundResult =
                orderRemoteGateway.refund(
                        new RefundCreateDto(po.getPayId(), refundAmt, null, refundRequestNo));
        if (refundResult == null
                || !CommonCode.SUCCESS.equals(refundResult.getCode())
                || !Boolean.TRUE.equals(refundResult.getData())) {
            log.error("支付退款失败，orderId={}，payId={}", po.getId(), po.getPayId());
            throw new BizException(OrderCodeEnum.ORDER_REFUND_FAIL);
        }
    }

    /**
     * 解析并锁定一次退款请求号。
     *
     * <p>已有请求号必须与候选值一致；否则使用候选值或默认 R<orderId>，避免重复退款被当成新请求。
     *
     * @param po 订单持久化对象
     * @param candidate 候选时间，可能早于、等于或晚于上界
     * @return 本次退款稳定请求号。
     * @throws BizException 已有请求号与候选值冲突。
     */
    private String resolveRefundRequestNo(OrderPo po, String candidate) {
        String current = po.getRefundRequestNo();
        if (current != null && !current.isBlank()) {
            if (candidate != null && !candidate.isBlank() && !current.equals(candidate)) {
                throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
            }
            return current;
        }
        String resolved = candidate == null || candidate.isBlank() ? "R" + po.getId() : candidate;
        po.setRefundRequestNo(resolved);
        return resolved;
    }

    /**
     * 查询用户订单的物流节点。
     *
     * <p>校验订单归属后按创建时间升序转换节点，不调用外部物流服务。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 按时间排序的物流节点列表。
     * @throws BizException 订单不存在或用户不匹配。
     */
    @Override
    public List<OrderLogisticsVo> queryLogistics(Long orderId, Long userId) {
        OrderPo po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        List<OrderLogisticsPo> nodes =
                orderLogisticsManager.list(
                        Wrappers.lambdaQuery(OrderLogisticsPo.class)
                                .eq(OrderLogisticsPo::getOrderId, orderId)
                                .orderByAsc(OrderLogisticsPo::getCreateTime));
        return nodes.stream().map(this::toLogisticsVo).collect(Collectors.toList());
    }

    /**
     * 把物流持久化节点转换为用户视图。
     *
     * <p>仅复制节点、描述和创建时间，不暴露订单 ID。
     *
     * @param po 订单物流持久化对象
     * @return 物流节点视图。
     */
    private OrderLogisticsVo toLogisticsVo(OrderLogisticsPo po) {
        OrderLogisticsVo vo = new OrderLogisticsVo();
        vo.setNode(po.getNode());
        vo.setDescription(po.getDescription());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 由调度器触发支付超时订单扫描。
     *
     * <p>使用 Redis 锁保证单实例执行，业务处理委托 OrderService。
     *
     * @return 本次扫描或处理的数量。
     */
    @Override
    public int scanExpiredOrders() {
        int count = 0;
        long lastId = 0L;

        // 批量处理，避免单次扫描耗时过长
        for (int batch = 0; batch < SCAN_MAX_BATCHES; batch++) {
            // 获取待超时订单
            List<OrderPo> expiredOrder =
                    orderManager.listExpiredPendingOrders(
                            OrderStatusEnum.PENDING.intCode(), lastId, SCAN_BATCH_SIZE);

            // 超时订单为空，直接返回
            if (expiredOrder.isEmpty()) {
                break;
            }

            for (OrderPo po : expiredOrder) {
                try {
                    // 设置库存释放状态为待处理
                    po.setStockReleaseStatus(StockReleaseStatusEnum.PENDING.intCode());
                    // 设置订单状态为取消
                    transition(po, OrderStatusEnum.CANCELED, "SYSTEM_TIMEOUT", "SYSTEM_TIMEOUT", null);
                    boolean released = releaseStockForOrder(po);

                    // 标记库存释放结果
                    markStockReleaseResult(po, released);
                    if (!released) {
                        log.error("库存释放待处理，orderId={}", po.getId());
                    }
                    count++;
                } catch (Exception e) {
                    log.warn("scanExpiredOrders 取消失败，orderId={}，err={}", po.getId(), e.getMessage());
                }
            }
            lastId = expiredOrder.get(expiredOrder.size() - 1).getId();
            if (expiredOrder.size() < SCAN_BATCH_SIZE) {
                break;
            }
        }
        if (count > 0) {
            log.info("scanExpiredOrders 已取消 {} 个订单", count);
        }
        return count;
    }

    /**
     * 由调度器触发待释放库存补偿。
     *
     * <p>使用独立 Redis 锁并按批扫描，将具体释放逻辑交给 OrderService。
     *
     * @return 本次扫描或处理的数量。
     */
    @Override
    public int scanStockReleasePending() {
        List<OrderPo> pending =
                orderManager.list(
                        Wrappers.lambdaQuery(OrderPo.class)
                                .eq(OrderPo::getStatus, OrderStatusEnum.CANCELED.intCode())
                                .eq(OrderPo::getStockReleaseStatus, StockReleaseStatusEnum.PENDING.intCode())
                                .orderByAsc(OrderPo::getUpdateTime)
                                .orderByAsc(OrderPo::getId)
                                .last("LIMIT " + SCAN_BATCH_SIZE));
        int count = 0;
        for (OrderPo po : pending) {
            if (releaseStockForOrder(po)) {
                markStockReleaseResult(po, true);
                count++;
            } else {
                // 失败时刷新 updateTime，让该订单排到队尾，避免故障单长期占据批次造成饥饿
                markStockReleaseResult(po, false);
                log.error("库存释放重试失败，orderId={}", po.getId());
            }
        }
        return count;
    }

    /**
     * 由调度器触发已发货订单自动确认。
     *
     * <p>使用独立 Redis 锁，避免多实例重复推进订单状态。
     *
     * @return 本次扫描或处理的数量。
     */
    @Override
    public int scanAutoConfirm() {
        LocalDateTime confirmThreshold = LocalDateTime.now().minusDays(AUTO_CONFIRM_DAYS);
        int count = 0;
        long lastId = 0L;
        for (int batch = 0; batch < SCAN_MAX_BATCHES; batch++) {
            List<OrderPo> overdue =
                    orderManager.list(
                            Wrappers.lambdaQuery(OrderPo.class)
                                    .eq(OrderPo::getStatus, OrderStatusEnum.SHIPPED.intCode())
                                    .lt(OrderPo::getShippedAt, confirmThreshold)
                                    .gt(OrderPo::getId, lastId)
                                    .orderByAsc(OrderPo::getId)
                                    .last("LIMIT " + SCAN_BATCH_SIZE));
            if (overdue.isEmpty()) {
                break;
            }
            for (OrderPo po : overdue) {
                try {
                    transitStatus(po.getId(), OrderStatusEnum.COMPLETED, "SYSTEM_AUTO_CONFIRM", null);
                    count++;
                } catch (Exception e) {
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
