package com.yirancrazy.minimall.order.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderLogisticsPO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderLogisticsManager;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务实现，编排下单核心链路：取商品快照→锁库存→创建支付流水→持久化订单，并通过事件总线广播订单状态变更。
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderManager orderManager;
    private final OrderLogisticsManager orderLogisticsManager;
    private final GoodsFeignClient goodsFeignClient;
    private final StockFeignClient stockFeignClient;
    private final PayFeignClient payFeignClient;
    private final EventBus eventBus;

    public OrderServiceImpl(OrderManager orderManager,
                            OrderLogisticsManager orderLogisticsManager,
                            GoodsFeignClient goodsFeignClient,
                            StockFeignClient stockFeignClient,
                            PayFeignClient payFeignClient,
                            EventBus eventBus) {
        this.orderManager = orderManager;
        this.orderLogisticsManager = orderLogisticsManager;
        this.goodsFeignClient = goodsFeignClient;
        this.stockFeignClient = stockFeignClient;
        this.payFeignClient = payFeignClient;
        this.eventBus = eventBus;
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
        po.setUserId(userId);
        po.setSkuId(skuId);
        po.setQuantity(quantity);
        po.setAmount(amount);
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

    @Override
    public void pay(Long orderId) {
        OrderPO po = getOrder(orderId);
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, po.getUserId(), po.getAmount(), LocalDateTime.now().toString());
        eventBus.publishInTx(event,
            () -> transitStatus(orderId, OrderStatusEnum.PAID),
            e -> isOrderPaid(orderId));
        log.info("order paid, orderId={}", orderId);
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
        transitStatus(orderId, OrderStatusEnum.CANCELLED);
        Boolean released = stockFeignClient.release(
            new StockReserveDTO(po.getSkuId(), po.getQuantity())).getData();
        if (released == null || !released) {
            log.warn("stock release failed on cancel, orderId={}", orderId);
        }
        log.info("order cancelled, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void ship(Long orderId, Long merchantId) {
        transitStatus(orderId, OrderStatusEnum.SHIPPED);
        OrderLogisticsPO node = new OrderLogisticsPO();
        node.setOrderId(orderId);
        node.setNode("已发货");
        node.setDescription("商家已发货");
        node.setCreatedTime(LocalDateTime.now());
        orderLogisticsManager.save(node);
        log.info("order shipped, orderId={}, merchantId={}", orderId, merchantId);
    }

    @Override
    public void confirm(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.RECEIVED);
        log.info("order confirmed, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void refund(Long orderId) {
        OrderPO po = getOrder(orderId);
        OrderStatusEnum current = fromCode(po.getStatus());
        if (!current.canTransitTo(OrderStatusEnum.REFUNDING)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        po.setRefundFromStatus(po.getStatus());
        po.setStatus(OrderStatusEnum.REFUNDING.intCode());
        orderManager.updateById(po);

        Boolean refunded = payFeignClient.refund(
            new RefundCreateDTO(po.getPayId(), po.getAmount(), null)).getData();
        if (refunded == null || !refunded) {
            log.warn("pay refund failed, orderId={}", orderId);
        }
        log.info("order refunding, orderId={}", orderId);
    }

    @Override
    public void handleRefundCallback(Long orderId, boolean success) {
        OrderPO po = getOrder(orderId);
        if (success) {
            po.setStatus(OrderStatusEnum.REFUNDED.intCode());
            log.info("order refunded, orderId={}", orderId);
        }
        else {
            Integer fromStatus = po.getRefundFromStatus();
            po.setStatus(fromStatus != null ? fromStatus : OrderStatusEnum.PAID.intCode());
            log.info("order refund failed, reverted, orderId={}", orderId);
        }
        orderManager.updateById(po);
    }

    @Override
    public Integer getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
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
        transitStatus(orderId, OrderStatusEnum.CANCELLED);
        Boolean released = stockFeignClient.release(
            new StockReserveDTO(po.getSkuId(), po.getQuantity())).getData();
        if (released == null || !released) {
            log.warn("stock release failed on merchant-close, orderId={}", orderId);
        }
        log.info("order merchant-closed, orderId={}, merchantId={}", orderId, merchantId);
    }

    /**
     * 用户删除订单，仅允许终态（CANCELLED/RECEIVED/REFUNDED）删除，归属不符抛出 ORDER_NOT_FOUND。
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
        if (!status.equals(OrderStatusEnum.CANCELLED.intCode())
            && !status.equals(OrderStatusEnum.RECEIVED.intCode())
            && !status.equals(OrderStatusEnum.REFUNDED.intCode())) {
            throw new BizException(OrderCodeEnum.ORDER_DELETE_NOT_ALLOWED);
        }
        orderManager.removeById(orderId);
        log.info("order deleted, orderId={}, userId={}", orderId, userId);
    }

    /**
     * 平台强制关闭异常订单，仅允许 PENDING 状态关闭并释放库存。
     * @param orderId 订单ID
     */
    @Override
    public void platformClose(Long orderId) {
        OrderPO po = getOrder(orderId);
        transitStatus(orderId, OrderStatusEnum.CANCELLED);
        Boolean released = stockFeignClient.release(
            new StockReserveDTO(po.getSkuId(), po.getQuantity())).getData();
        if (released == null || !released) {
            log.warn("stock release failed on platform-close, orderId={}", orderId);
        }
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

    private void transitStatus(Long orderId, OrderStatusEnum target) {
        OrderPO po = getOrder(orderId);
        OrderStatusEnum current = fromCode(po.getStatus());
        if (!current.canTransitTo(target)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        po.setStatus(target.intCode());
        orderManager.updateById(po);
    }

    private OrderStatusEnum fromCode(Integer code) {
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getCode().equals(String.valueOf(code))) {
                return e;
            }
        }
        throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
    }

    /**
     * 分页查询订单，按 userId/merchantId/status 等值过滤，按创建时间倒序。
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    @Override
    public IPage<OrderPO> page(OrderPageDTO dto) {
        Page<OrderPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return orderManager.page(page, Wrappers.lambdaQuery(OrderPO.class)
            .eq(dto.getUserId() != null, OrderPO::getUserId, dto.getUserId())
            .eq(dto.getMerchantId() != null, OrderPO::getMerchantId, dto.getMerchantId())
            .eq(dto.getStatus() != null, OrderPO::getStatus, dto.getStatus())
            .orderByDesc(OrderPO::getCreateTime));
    }

    /**
     * 商家审核退款，校验订单归属与 REFUNDING 状态；approved=false 回退到 refundFromStatus，true 仅记录审核通过。
     * @param orderId 订单ID
     * @param approved 是否同意退款
     * @param merchantId 商家ID
     */
    @Override
    public void reviewRefund(Long orderId, boolean approved, Long merchantId) {
        OrderPO po = getOrder(orderId);
        if (!po.getMerchantId().equals(merchantId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (po.getStatus() != OrderStatusEnum.REFUNDING.intCode()) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_REFUNDING);
        }
        if (!approved) {
            Integer fromStatus = po.getRefundFromStatus();
            po.setStatus(fromStatus != null ? fromStatus : OrderStatusEnum.PAID.intCode());
            orderManager.updateById(po);
            log.info("refund rejected by merchant, orderId={}, merchantId={}", orderId, merchantId);
            return;
        }
        log.info("refund approved by merchant, orderId={}, merchantId={}", orderId, merchantId);
    }

    /**
     * 平台退款仲裁，校验 REFUNDING 状态；approved=true 强制推进 REFUNDED，false 回退到 refundFromStatus。
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
            po.setStatus(OrderStatusEnum.REFUNDED.intCode());
            orderManager.updateById(po);
            log.info("refund arbitrated approved, orderId={}", orderId);
            return;
        }
        Integer fromStatus = po.getRefundFromStatus();
        po.setStatus(fromStatus != null ? fromStatus : OrderStatusEnum.PAID.intCode());
        orderManager.updateById(po);
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
}
