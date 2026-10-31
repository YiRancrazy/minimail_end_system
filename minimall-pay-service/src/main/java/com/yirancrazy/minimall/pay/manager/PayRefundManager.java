package com.yirancrazy.minimall.pay.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义退款流水的 Manager 数据访问契约。
 * 提供幂等查询、待补偿列表、原子抢占、状态更新和金额汇总，供退款编排使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PayRefundManager extends IService<PayRefundPo> {

  /**
   * 按退款请求号查询未删除的退款流水，用于幂等复用。
   *
   * @param refundRequestNo 调用方退款请求号
   * @return 已存在的退款流水；不存在时返回 null
   */
  PayRefundPo findByRefundRequestNo(String refundRequestNo);

  /**
   * 查询已到重试时间且尚未完成的退款流水。
   *
   * @param now 当前时间，作为重试时间上界
   * @param limit 单批最大条数
   * @return 按 ID 升序排列的待退款流水
   */
  List<PayRefundPo> listPendingRefunds(LocalDateTime now, int limit);

  /**
   * 以重试时间租约原子抢占待退款流水，并累加重试次数。
   *
   * @param refundNo 退款单号
   * @param now 当前时间，用于确认租约已到期
   * @param leaseUntil 本次处理占用截止时间
   * @return 1 表示抢占成功，0 表示已被其他任务处理
   */
  int claimPendingRefund(String refundNo, LocalDateTime now, LocalDateTime leaseUntil);

  /**
   * 仅在退款流水的原状态匹配时更新状态，并写入渠道退款交易号。
   *
   * @param refundNo 退款单号
   * @param fromStatus 期望的原退款状态 code
   * @param toStatus 目标退款状态 code
   * @param refundTradeNo 渠道退款交易号
   * @return 实际更新行数
   */
  int updateStatusIf(String refundNo, int fromStatus, int toStatus, String refundTradeNo);

  /**
   * 更新待退款流水的下次重试时间并累加重试次数。
   *
   * @param refundNo 退款单号
   * @param nextRetryAt 下次可处理时间
   * @return 实际更新行数
   */
  int scheduleRefundRetry(String refundNo, LocalDateTime nextRetryAt);

  /**
   * 汇总支付单下待处理及成功退款的已占用金额，用于防止累计超退。
   *
   * @param paymentNo 支付单号
   * @return 已占用退款金额；无记录时返回 0
   */
  BigDecimal sumCommittedAmount(String paymentNo);
}
