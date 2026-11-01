package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import com.yirancrazy.minimall.pay.mapper.PayRefundMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现退款流水的 MyBatis-Plus 数据访问编排。
 * 封装幂等查询、待退款抢占、条件状态更新、重试排期和已占用金额汇总。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class PayRefundManagerImpl extends ServiceImpl<PayRefundMapper, PayRefundPo>
        implements PayRefundManager {

    /**
     * 委托 Mapper 按退款请求号查询有效退款流水。
     *
     * @param refundRequestNo 调用方退款请求号
     * @return 已存在的退款流水；不存在时返回 null
     */
    @Override
    public PayRefundPo findByRefundRequestNo(String refundRequestNo) {
        return baseMapper.findByRefundRequestNo(refundRequestNo);
    }

    /**
     * 委托 Mapper 查询已到重试时间的待退款流水。
     *
     * @param now 当前时间
     * @param limit 单批最大条数
     * @return 按 ID 升序排列的待退款流水
     */
    @Override
    public List<PayRefundPo> listPendingRefunds(LocalDateTime now, int limit) {
        return baseMapper.selectPendingRefunds(now, limit);
    }

    /**
     * 执行带租约条件的退款流水原子抢占。
     *
     * @param refundNo 退款单号
     * @param now 当前时间
     * @param leaseUntil 本次处理占用截止时间
     * @return 1 表示抢占成功，0 表示抢占失败
     */
    @Override
    public int claimPendingRefund(String refundNo, LocalDateTime now, LocalDateTime leaseUntil) {
        return baseMapper.claimPendingRefund(refundNo, now, leaseUntil);
    }

    /**
     * 执行带原状态条件的退款状态更新。
     *
     * @param refundNo 退款单号
     * @param fromStatus 期望的原退款状态 code
     * @param toStatus 目标退款状态 code
     * @param refundTradeNo 渠道退款交易号
     * @return 实际更新行数
     */
    @Override
    public int updateStatusIf(String refundNo, int fromStatus, int toStatus, String refundTradeNo) {
        return baseMapper.updateStatusIf(refundNo, fromStatus, toStatus, refundTradeNo);
    }

    /**
     * 委托 Mapper 更新退款重试时间并累加次数。
     *
     * @param refundNo 退款单号
     * @param nextRetryAt 下次可处理时间
     * @return 实际更新行数
     */
    @Override
    public int scheduleRefundRetry(String refundNo, LocalDateTime nextRetryAt) {
        return baseMapper.scheduleRefundRetry(refundNo, nextRetryAt);
    }

    /**
     * 委托 Mapper 汇总支付单下已提交的退款金额。
     *
     * @param paymentNo 支付单号
     * @return 待处理与成功退款金额之和
     */
    @Override
    public BigDecimal sumCommittedAmount(String paymentNo) {
        return baseMapper.sumCommittedAmount(paymentNo);
    }
}
