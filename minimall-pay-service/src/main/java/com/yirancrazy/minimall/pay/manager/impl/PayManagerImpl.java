package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDto;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDto;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现支付交易表的 MyBatis-Plus 数据访问编排。
 * 向服务层提供支付统计、条件状态更新和对账单聚合，不包含支付业务决策。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class PayManagerImpl extends ServiceImpl<PayTransactionMapper, PayTransactionPo>
        implements PayManager {

    /**
     * 查询订单号对应的最新支付流水。
     *
     * @param orderNo 业务订单号
     * @return 最新支付流水；不存在时返回 null
     */
    @Override
    public PayTransactionPo findLatestByOrderNo(String orderNo) {
        return getOne(
                Wrappers.<PayTransactionPo>lambdaQuery()
                        .eq(PayTransactionPo::getOrderNo, orderNo)
                        .orderByDesc(PayTransactionPo::getId)
                        .last("LIMIT 1"));
    }

    /**
     * 委托 Mapper 聚合支付成功金额、退款金额和交易笔数。
     *
     * @param merchantId 商家筛选条件，可为空
     * @param startTime 创建时间下界，可为空
     * @param endTime 创建时间上界，可为空
     * @return 支付统计数据库结果
     */
    @Override
    public PayStatisticsDto statistics(
            Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statistics(merchantId, startTime, endTime);
    }

    /**
     * 执行带原状态条件的支付单状态更新。
     *
     * @param paymentNo 支付单号
     * @param fromStatus 期望的原状态 code
     * @param toStatus 目标状态 code
     * @return 实际更新行数
     */
    @Override
    public int updateStatusIf(String paymentNo, int fromStatus, int toStatus) {
        return baseMapper.updateStatusIf(paymentNo, fromStatus, toStatus);
    }

    /**
     * 执行待支付至已关闭的条件状态更新，并原子释放活跃订单号。
     *
     * @param paymentNo 支付单号
     * @param pendingStatus 待支付状态 code
     * @param closedStatus 已关闭状态 code
     * @return 实际更新行数
     */
    @Override
    public int closePendingPayment(String paymentNo, int pendingStatus, int closedStatus) {
        return baseMapper.closePendingPayment(paymentNo, pendingStatus, closedStatus);
    }

    /**
     * 执行待支付至成功的条件状态更新，并原子写入渠道支付信息。
     *
     * @param paymentNo 支付单号
     * @param tradeNo 渠道交易号
     * @param channelResponse 渠道原始响应
     * @param paidAt 支付成功时间
     * @param pendingStatus 待支付状态 code
     * @param successStatus 支付成功状态 code
     * @return 实际更新行数
     */
    @Override
    public int markSuccessIfPending(
            String paymentNo,
            String tradeNo,
            String channelResponse,
            LocalDateTime paidAt,
            int pendingStatus,
            int successStatus) {
        return baseMapper.markSuccessIfPending(
                paymentNo, tradeNo, channelResponse, paidAt, pendingStatus, successStatus);
    }

    /**
     * 委托 Mapper 聚合指定时间范围内的对账单数据。
     *
     * @param startTime 统计下界
     * @param endTime 统计上界
     * @return 支付对账单数据库结果
     */
    @Override
    public PayStatementResultDto statement(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statement(startTime, endTime);
    }
}
