package com.yirancrazy.minimall.pay.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;
import com.yirancrazy.minimall.common.security.EncryptedStringTypeHandler;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayTransactionPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_pay_transaction", autoResultMap = true)
public class PayTransactionPO extends BasePO {
    private String paymentNo;
    private String tradeNo;
    private String orderNo;
    private String activeOrderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private Integer status;
    private Integer channel;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String channelResponse;

    private LocalDateTime paidAt;
    private LocalDateTime expireAt;
    private String idempotencyKey;
}
