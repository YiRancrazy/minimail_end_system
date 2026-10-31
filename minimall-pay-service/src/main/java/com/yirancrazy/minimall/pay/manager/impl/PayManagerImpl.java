package com.yirancrazy.minimall.pay.manager.impl;

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
