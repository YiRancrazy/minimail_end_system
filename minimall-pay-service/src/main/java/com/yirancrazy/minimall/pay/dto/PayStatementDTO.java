package com.yirancrazy.minimall.pay.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 对账单查询入参，支持按日期范围聚合统计支付流水
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class PayStatementDTO {

    /** 起始日期（含），格式 yyyy-MM-dd */
    @NotNull(message = "startDate cannot be null")
    private LocalDate startDate;

    /** 截止日期（含），格式 yyyy-MM-dd */
    @NotNull(message = "endDate cannot be null")
    private LocalDate endDate;
}
