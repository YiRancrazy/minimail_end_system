package com.yirancrazy.minimall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_order，保存订单主记录、金额、收货快照、状态和退款汇总。
 *   业务边界：订单主状态、库存释放状态和退款来源状态分别持久化；复杂迁移与补偿由 OrderServiceImpl 编排。
 *   使用场景：订单创建、状态推进、分页、统计、导出和事务性事件记录均依赖该实体。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class OrderPo extends BasePo {
  private String orderNo;
  private Integer orderType;
  private Long userId;
  private Long merchantId;
  private Long payId;
  private Long skuId;
  private Integer quantity;
  private BigDecimal amount;
  private BigDecimal totalAmount;
  private BigDecimal payAmount;
  private BigDecimal freightAmount;
  private BigDecimal discountAmount;
  private String receiverSnapshotJson;
  private LocalDateTime payExpireAt;
  private LocalDateTime paidAt;
  private LocalDateTime receivedAt;
  private LocalDateTime closedAt;
  private String closeReason;
  private String clientIp;
  private String idempotencyKey;
  private Integer status;
  private Integer stockReleaseStatus;
  private Integer refundFromStatus;

  private BigDecimal refundAmount;

  private BigDecimal totalRefundedAmount;

  private String refundRequestNo;
  private LocalDateTime shippedAt;
  private String orderGroupNo;
}
