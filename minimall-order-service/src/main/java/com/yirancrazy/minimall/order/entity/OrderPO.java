package com.yirancrazy.minimall.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order持久化对象，映射t_order表，承载订单生命周期关键字段（单号/金额/快照/状态时间戳）。
 * @Version: 1.3
 * @DateTime: 2026/08/13
 */
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
    private Integer refundFromStatus;
    /** 本次申请退款金额，部分退款场景使用；null 表示全单退款 */
    private BigDecimal refundAmount;
    private LocalDateTime shippedAt;
    private String orderGroupNo;
}
