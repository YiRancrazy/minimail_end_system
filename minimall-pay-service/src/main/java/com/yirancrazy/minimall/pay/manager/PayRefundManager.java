package com.yirancrazy.minimall.pay.manager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRefundManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PayRefundManager extends IService<PayRefundPO> {


    /**
     * 执行 findByRefundRequestNo 方法。
     * @param refundRequestNo refundRequestNo 参数
     * @return 处理结果
     */
    PayRefundPO findByRefundRequestNo(String refundRequestNo);


    /**
     * 查询列表数据。
     * @param now now 参数
     * @param limit limit 参数
     * @return 结果列表
     */
    List<PayRefundPO> listPendingRefunds(LocalDateTime now, int limit);


    /**
     * 执行 claimPendingRefund 方法。
     * @param refundNo refundNo 参数
     * @param now now 参数
     * @param leaseUntil leaseUntil 参数
     * @return 业务数值
     */
    int claimPendingRefund(String refundNo, LocalDateTime now, LocalDateTime leaseUntil);


    /**
     * 更新业务数据。
     * @param refundNo refundNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @param refundTradeNo refundTradeNo 参数
     * @return 业务数值
     */
    int updateStatusIf(String refundNo, int fromStatus, int toStatus, String refundTradeNo);


    /**
     * 执行 scheduleRefundRetry 方法。
     * @param refundNo refundNo 参数
     * @param nextRetryAt nextRetryAt 参数
     * @return 业务数值
     */
    int scheduleRefundRetry(String refundNo, LocalDateTime nextRetryAt);


    /**
     * 执行 sumCommittedAmount 方法。
     * @param paymentNo paymentNo 参数
     * @return 处理结果
     */
    BigDecimal sumCommittedAmount(String paymentNo);
}
