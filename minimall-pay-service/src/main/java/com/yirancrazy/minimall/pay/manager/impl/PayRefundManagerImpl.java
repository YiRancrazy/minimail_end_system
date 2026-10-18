package com.yirancrazy.minimall.pay.manager.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRefundManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class PayRefundManagerImpl extends ServiceImpl<PayRefundMapper, PayRefundPO>
    implements PayRefundManager {


    /**
     * 执行 findByRefundRequestNo 方法。
     * @param refundRequestNo refundRequestNo 参数
     * @return 处理结果
     */
    @Override
    public PayRefundPO findByRefundRequestNo(String refundRequestNo) {
        return baseMapper.findByRefundRequestNo(refundRequestNo);
    }


    /**
     * 查询列表数据。
     * @param now now 参数
     * @param limit limit 参数
     * @return 结果列表
     */
    @Override
    public List<PayRefundPO> listPendingRefunds(LocalDateTime now, int limit) {
        return baseMapper.selectPendingRefunds(now, limit);
    }


    /**
     * 执行 claimPendingRefund 方法。
     * @param refundNo refundNo 参数
     * @param now now 参数
     * @param leaseUntil leaseUntil 参数
     * @return 业务数值
     */
    @Override
    public int claimPendingRefund(String refundNo, LocalDateTime now, LocalDateTime leaseUntil) {
        return baseMapper.claimPendingRefund(refundNo, now, leaseUntil);
    }


    /**
     * 更新业务数据。
     * @param refundNo refundNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @param refundTradeNo refundTradeNo 参数
     * @return 业务数值
     */
    @Override
    public int updateStatusIf(String refundNo, int fromStatus, int toStatus, String refundTradeNo) {
        return baseMapper.updateStatusIf(refundNo, fromStatus, toStatus, refundTradeNo);
    }


    /**
     * 执行 scheduleRefundRetry 方法。
     * @param refundNo refundNo 参数
     * @param nextRetryAt nextRetryAt 参数
     * @return 业务数值
     */
    @Override
    public int scheduleRefundRetry(String refundNo, LocalDateTime nextRetryAt) {
        return baseMapper.scheduleRefundRetry(refundNo, nextRetryAt);
    }


    /**
     * 执行 sumCommittedAmount 方法。
     * @param paymentNo paymentNo 参数
     * @return 处理结果
     */
    @Override
    public BigDecimal sumCommittedAmount(String paymentNo) {
        return baseMapper.sumCommittedAmount(paymentNo);
    }
}
