package com.yirancrazy.minimall.pay.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义平台支付对账单的日期区间。
 * 起始和结束日期均必填，由服务层转换为左闭右开的日粒度时间范围。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class PayStatementDto {

    @NotNull(message = "startDate cannot be null")
    private LocalDate startDate;

    @NotNull(message = "endDate cannot be null")
    private LocalDate endDate;
}
