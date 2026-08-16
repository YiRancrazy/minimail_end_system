package com.yirancrazy.minimall.merchant.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请入参，金额单位为元（CNY），精度到分。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Data
public class WithdrawApplyDTO {

    @NotNull
    @DecimalMin(value = "0.01", message = "提现金额必须大于 0")
    @DecimalMax(value = "100000000", message = "提现金额不得超出 1 亿元上限")
    @Digits(integer = 10, fraction = 2, message = "提现金额整数最多 10 位、小数 2 位")
    private BigDecimal amount;
}
