package com.yirancrazy.minimall.order.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务接口，定义Order相关业务契约
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
public interface OrderService {
    Long create(Long userId, Long skuId, Integer quantity);

    void pay(Long orderId);

    void cancel(Long orderId, Long userId);

    void ship(Long orderId, Long merchantId);

    void confirm(Long orderId, Long userId);

    void refund(Long orderId);

    void handleRefundCallback(Long orderId, boolean success);

    Integer getStatus(Long orderId);

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
     * 用户删除订单，仅允许终态（CANCELLED/RECEIVED/REFUNDED）删除，归属不符抛出 ORDER_NOT_FOUND。
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
     * 分页查询订单，支持按用户/商家/状态过滤。
     * @param dto 分页查询入参
     * @return 订单分页结果
     */
    IPage<OrderPO> page(OrderPageDTO dto);

    /**
     * 商家审核退款，仅允许 REFUNDING 状态订单；approved=false 回退到 refundFromStatus，true 仅记录审核通过。
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
}
