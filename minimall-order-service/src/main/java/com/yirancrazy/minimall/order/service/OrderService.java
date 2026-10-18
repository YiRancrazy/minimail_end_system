package com.yirancrazy.minimall.order.service;

import java.util.List;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.dto.ReceiverDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.vo.OrderExportVO;
import com.yirancrazy.minimall.order.vo.OrderItemVO;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;
import com.yirancrazy.minimall.order.vo.OrderSummaryVO;
import com.yirancrazy.minimall.order.vo.OrderTrendVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OrderService {
    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 业务数值
     */
    Long create(Long userId, Long skuId, Integer quantity);


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param receiver receiver 参数
     * @return 业务数值
     */
    Long create(Long userId, Long skuId, Integer quantity, ReceiverDTO receiver);


    /**
     * 校验业务条件。
     * @param userId userId 参数
     * @param items items 参数
     * @return 业务数值
     */
    Long checkout(Long userId, List<OrderCheckoutItemDTO> items);


    /**
     * 校验业务条件。
     * @param userId userId 参数
     * @param items items 参数
     * @param receiver receiver 参数
     * @return 业务数值
     */
    Long checkout(Long userId, List<OrderCheckoutItemDTO> items, ReceiverDTO receiver);


    /**
     * 执行 pay 方法。
     * @param orderId orderId 参数
     */
    void pay(Long orderId);


    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     */
    void payByOrderNo(String orderNo);

    /**
     * 执行 cancel 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    void cancel(Long orderId, Long userId);

    /**
     * 执行 ship 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param carrier carrier 参数
     * @param trackingNo trackingNo 参数
     */
    void ship(Long orderId, Long merchantId, String carrier, String trackingNo);

    /**
     * 执行 confirm 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    void confirm(Long orderId, Long userId);


    /**
     * 执行 refund 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    void refund(Long orderId, Long userId);


    /**
     * 执行 merchantInitiateRefund 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     */
    void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason);


    /**
     * 执行 merchantInitiateRefund 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     * @param refundRequestNo refundRequestNo 参数
     */
    void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason,
                                String refundRequestNo);


    /**
     * 执行 merchantInitiateRefundByNo 方法。
     * @param refundNo refundNo 参数
     * @param merchantId merchantId 参数
     * @param refundAmount refundAmount 参数
     * @param reason reason 参数
     */
    void merchantInitiateRefundByNo(String refundNo, Long merchantId, String refundAmount, String reason);


    /**
     * 处理业务事件。
     * @param dto dto 参数
     */
    void handleRefundCallback(RefundCallbackDTO dto);


    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 处理结果
     */
    OrderCommentEligibilityDTO commentEligibility(String orderNo, Long userId, Long spuId);


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    OrderPO getDetail(Long orderId, Long userId);
    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 业务数值
     */
    Integer getStatus(Long orderId, Long userId);


    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @return 处理结果
     */
    OrderPO getDetail(Long orderId);
    /**
     * 获取相关数据。
     * @param orderId orderId 参数
     * @return 业务数值
     */
    Integer getStatus(Long orderId);


    /**
     * 执行 resolveMerchantId 方法。
     * @param ref ref 参数
     * @return 业务数值
     */
    Long resolveMerchantId(String ref);


    /**
     * 获取相关数据。
     * @param ref ref 参数
     * @return 处理结果
     */
    OrderPaySnapshotDTO getPaySnapshot(String ref);




    /**
     * 查询列表数据。
     * @param orderId orderId 参数
     * @return 结果列表
     */
    List<OrderItemVO> listItemVO(Long orderId);


    /**
     * 执行 merchantClose 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     */
    void merchantClose(Long orderId, Long merchantId);


    /**
     * 删除业务数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     */
    void delete(Long orderId, Long userId);


    /**
     * 执行 platformClose 方法。
     * @param orderId orderId 参数
     */
    void platformClose(Long orderId);


    /**
     * 执行 pendingCount 方法。
     * @param merchantId merchantId 参数
     * @return 业务数值
     */
    long pendingCount(Long merchantId);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<OrderPO> page(OrderPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<OrderVO> pageVO(OrderPageDTO dto);


    /**
     * 执行 detailVO 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    OrderVO detailVO(Long orderId, Long userId);


    /**
     * 执行 platformDetailVO 方法。
     * @param orderId orderId 参数
     * @return 处理结果
     */
    OrderVO platformDetailVO(Long orderId);


    /**
     * 执行 platformExportList 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<OrderPO> platformExportList(OrderPageDTO dto);
    /**
     * 执行 merchantPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<OrderVO> merchantPageVO(OrderPageDTO dto);


    /**
     * 执行 merchantDetailVO 方法。
     * @param orderId orderId 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    OrderVO merchantDetailVO(Long orderId, Long merchantId);


    /**
     * 执行 reviewRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     */
    void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason);


    /**
     * 执行 reviewRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     * @param refundRequestNo refundRequestNo 参数
     */
    void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason,
                      String refundRequestNo);


    /**
     * 执行 reviewRefundByNo 方法。
     * @param refundNo refundNo 参数
     * @param approved approved 参数
     * @param merchantId merchantId 参数
     * @param reason reason 参数
     */
    void reviewRefundByNo(String refundNo, boolean approved, Long merchantId, String reason);


    /**
     * 执行 arbitrateRefund 方法。
     * @param orderId orderId 参数
     * @param approved approved 参数
     */
    void arbitrateRefund(Long orderId, boolean approved);


    /**
     * 执行 queryLogistics 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 结果列表
     */
    List<OrderLogisticsVO> queryLogistics(Long orderId, Long userId);



    /**
     * 执行 exportList 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 结果列表
     */
    List<OrderPO> exportList(Long merchantId, OrderPageDTO dto);
    /**
     * 执行 exportVOList 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 结果列表
     */
    List<OrderExportVO> exportVOList(Long merchantId, OrderPageDTO dto);



    /**
     * 执行 platformExportVOList 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<OrderExportVO> platformExportVOList(OrderPageDTO dto);


    /**
     * 执行 platformExportVOList 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    List<OrderExportVO> platformExportVOList(String startDate, String endDate);


    /**
     * 执行 statistics 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    OrderStatisticsVO statistics(OrderPageDTO dto);


    /**
     * 执行 orderSummary 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    OrderSummaryVO orderSummary(String startDate, String endDate, String merchantId);


    /**
     * 执行 orderTrend 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    OrderTrendVO orderTrend(String startDate, String endDate, String merchantId);


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    OrderStatusCountsVO countStatusByUser(Long userId);


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanExpiredOrders();


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanAutoConfirm();


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanStockReleasePending();
}
