package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请DTO，承载提现金额与申请说明。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawApplyDTO {

    @NotNull(message = "提现金额不能为空")
    @Positive(message = "提现金额必须大于0")
    private BigDecimal amount;

    private String reason;
}
