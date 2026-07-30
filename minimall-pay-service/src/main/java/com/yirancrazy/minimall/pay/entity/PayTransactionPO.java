package com.yirancrazy.minimall.pay.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 持久化对象，对应数据库表结构。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_pay_transaction")
public class PayTransactionPO extends BasePO {
    private String paymentNo;
    private String tradeNo;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private Integer status;
    private Integer channel;
    private String channelResponse;
    private LocalDateTime paidAt;
    private LocalDateTime expireAt;
    private String idempotencyKey;
}