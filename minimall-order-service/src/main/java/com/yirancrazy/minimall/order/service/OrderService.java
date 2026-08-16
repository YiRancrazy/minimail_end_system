package com.yirancrazy.minimall.order.service;

import java.util.List;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务接口，定义Order相关业务契约
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
public interface OrderService {
    Long create(Long userId, Long skuId, Integer quantity);

    /**
     * 多SKU结算下单：批量获取商品快照、锁库存、创建订单头与明细行、初始化支付流水。
     * @param userId 用户ID
     * @param items 结算明细列表
     * @return 订单ID
     */
    Long checkout(Long userId, List<OrderCheckoutItemDTO> items);

    void pay(Long orderId);

    /**
     * 按业务单号推进订单为已支付，供支付回调使用（C 端支付回调携带业务单号而非订单ID）。
     * @param orderNo 业务单号
     */
    void payByOrderNo(String orderNo);

    void cancel(Long orderId, Long userId);

    void ship(Long orderId, Long merchantId, String carrier, String trackingNo);

    void confirm(Long orderId, Long userId);

    /**
     * 用户申请退款：校验订单归属后仅置 REFUNDING 并记录原状态，不发起真实支付退款；
     * 真实退款由商家审核通过后调用支付网关触发，状态由支付回调驱动。
     * @param orderId 订单ID
     * @param userId 用户ID（归属校验，来自可信 Header）
     */
    void refund(Long orderId, Long userId);

    /**
     * 商家主动发起退款，仅允许 PAID/SHIPPED/COMPLETED 三种状态的订单。
     * 退款走与用户申请退款相同的链路，但默认直接置 REFUNDING，由商家审核后再到 REFUNDED。
     * @param orderId   订单ID
     * @param merchantId 商家ID（归属校验）
     * @param refundAmount 退款金额（字符串由调用方决定精度，内部转 BigDecimal）
     * @param reason 退款原因
     */
    void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason);

    /**
     * 接收支付服务退款结果回调：仅 REFUNDING 状态可受理（否则抛状态流转异常，天然幂等）；
     * success 推进 REFUNDED，失败回退到 refundFromStatus。
     * @param orderId 订单ID
     * @param success 退款是否成功
     */
    void handleRefundCallback(Long orderId, boolean success);

    Integer getStatus(Long orderId);

    /**
     * 按订单标识（订单ID或业务单号）解析订单归属商户ID，供支付服务归属收款商户。
     * @param ref 订单标识
     * @return 商户ID；订单不存在时返回 null
     */
    Long resolveMerchantId(String ref);

    /**
     * 查询订单详情，不存在时抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @return 订单持久化实体
     */
    OrderPO getDetail(Long orderId);

    /**
     * 商家关闭订单，仅允许 PENDING 状态关闭并释放库存，归属不符时抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @param merchantId 商家ID
     */
    void merchantClose(Long orderId, Long merchantId);

    /**
     * 用户删除订单，仅允许终态（CANCELED/COMPLETED/REFUNDED）删除，归属不符抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    void delete(Long orderId, Long userId);

    /**
     * 平台强制关闭异常订单，仅允许 PENDING 状态关闭并释放库存。
     * @param orderId 订单ID
     */
    void platformClose(Long orderId);

    /**
     * 商家待处理订单数量统计，包含 PENDING/PAID/REFUNDING 三种状态。
     * @param merchantId 商家ID
     * @return 待处理订单总数
     */
    long pendingCount(Long merchantId);

    /**
     * 游标分页查询订单，支持按用户/商家/状态过滤。
     * @param dto 游标分页查询入参
     * @return 订单游标分页结果
     */
    CursorPageVO<OrderPO> page(OrderPageDTO dto);

    /**
     * 商家审核退款，仅允许 REFUNDING 状态订单；approved=true 发起真实支付退款（状态由回调驱动到 REFUNDED），
     * false 回退到 refundFromStatus。
     * @param orderId 订单ID
     * @param approved 是否同意退款
     * @param merchantId 商家ID，来自可信 Header
     */
    void reviewRefund(Long orderId, boolean approved, Long merchantId);

    /**
     * 平台退款仲裁，仅允许 REFUNDING 状态订单；approved=true 强制推进 REFUNDED，false 回退到 refundFromStatus。
     * @param orderId 订单ID
     * @param approved 仲裁是否支持退款
     */
    void arbitrateRefund(Long orderId, boolean approved);

    /**
     * 查询订单物流轨迹，按创建时间正序返回；订单不存在抛出 ORDER_NOT_FOUND。
     * @param orderId 订单ID
     * @return 物流节点列表
     */
    List<OrderLogisticsVO> queryLogistics(Long orderId);

    /**
     * 导出商家订单列表，最多 10000 行，merchantId 强制绑定。
     * @param merchantId 商家ID
     * @param dto 查询入参（复用过滤字段）
     * @return 订单列表
     */
    List<OrderPO> exportList(Long merchantId, OrderPageDTO dto);

    /**
     * 导出全平台订单列表，最多 10000 行，不绑定 merchantId。
     * @param dto 查询入参
     * @return 订单列表
     */
    List<OrderPO> platformExportList(OrderPageDTO dto);

    /**
     * 平台订单统计聚合，可选 merchantId 过滤，返回总数/总金额/退款金额/各状态计数。
     * @param dto 查询入参（复用 merchantId/startTime/endTime）
     * @return 订单统计VO
     */
    OrderStatisticsVO statistics(OrderPageDTO dto);

    /**
     * 统计指定用户各状态订单数量（待支付/待发货/待收货/已完成），供用户端"我的"页角标展示。
     * @param userId 用户ID
     * @return 各状态订单数量VO
     */
    OrderStatusCountsVO countStatusByUser(Long userId);

    /**
     * 扫描超时未支付订单，逐个推进 PENDING→CANCELED 并释放库存。由定时任务调度。
     * @return 本次扫描处理的订单数
     */
    int scanExpiredOrders();

    /**
     * 扫描发货后超期未确认收货订单，逐个推进 SHIPPED→COMPLETED。由定时任务调度。
     * @return 本次扫描处理的订单数
     */
    int scanAutoConfirm();
}
