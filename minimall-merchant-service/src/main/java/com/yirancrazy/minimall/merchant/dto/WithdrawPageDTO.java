package com.yirancrazy.minimall.merchant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请分页查询入参，游标分页。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Data
public class WithdrawPageDTO {

    private String cursor;

    @Min(1)
    @Max(100)
    private Integer limit = 20;

    private Integer status;

    private Long merchantId;
}
