package com.yirancrazy.minimall.pay.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现持久化对象，对应 t_merchant_withdraw 表。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merchant_withdraw")
public class MerchantWithdrawPO extends BasePO {
    private Long merchantId;
    private String withdrawNo;
    private BigDecimal amount;
    private Integer status;
    private String reason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;
}
