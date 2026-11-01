package com.yirancrazy.minimall.pay.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDto;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDto;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义支付交易表的 Manager 数据访问契约。
 * 向服务层暴露统计、条件状态推进和对账单聚合，隔离具体 Mapper SQL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PayManager extends IService<PayTransactionPo> {

  /**
   * 聚合指定时间范围内的支付成功金额、退款金额和交易笔数。
   * `merchantId` 为空时统计全平台。
   *
   * @param merchantId 商家筛选条件，可为空
   * @param startTime 创建时间下界，可为空
   * @param endTime 创建时间上界，可为空
   * @return 支付统计数据库结果
   */
  PayStatisticsDto statistics(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * 仅当支付单当前状态等于原状态时推进到目标状态。
   *
   * @param paymentNo 支付单号
   * @param fromStatus 期望的原状态 code
   * @param toStatus 目标状态 code
   * @return 实际更新行数，0 表示状态已被并发修改
   */
  int updateStatusIf(String paymentNo, int fromStatus, int toStatus);

  /**
   * 仅当支付单仍为待支付状态时关闭支付单并释放活跃订单号。
   *
   * @param paymentNo 支付单号
   * @param pendingStatus 待支付状态 code
   * @param closedStatus 已关闭状态 code
   * @return 实际更新行数，0 表示状态已被并发修改
   */
  int closePendingPayment(String paymentNo, int pendingStatus, int closedStatus);

  /**
   * 仅当支付单仍为待支付状态时写入成功状态和渠道支付信息。
   *
   * @param paymentNo 支付单号
   * @param tradeNo 渠道交易号
   * @param channelResponse 渠道原始响应
   * @param paidAt 支付成功时间
   * @param pendingStatus 待支付状态 code
   * @param successStatus 支付成功状态 code
   * @return 实际更新行数，0 表示状态已被并发修改
   */
  int markSuccessIfPending(
      String paymentNo,
      String tradeNo,
      String channelResponse,
      LocalDateTime paidAt,
      int pendingStatus,
      int successStatus);

  /**
   * 聚合对账单起止时间内的支付、退款和冻结数据。
   * 调用方传入的时间范围按左闭右开处理。
   *
   * @param startTime 统计下界
   * @param endTime 统计上界
   * @return 支付对账单数据库结果
   */
  PayStatementResultDto statement(LocalDateTime startTime, LocalDateTime endTime);
}
