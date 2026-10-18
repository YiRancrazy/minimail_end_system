package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class OrderPO extends BasePO {
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
