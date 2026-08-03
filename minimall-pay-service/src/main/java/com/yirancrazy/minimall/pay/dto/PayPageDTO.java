package com.yirancrazy.minimall.pay.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付流水分页查询DTO，支持按状态与时间范围过滤。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class PayPageDTO {

    @Min(value = 1)
    private Integer pageNo = 1;

    @Min(value = 1)
    @Max(value = 100)
    private Integer pageSize = 20;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
