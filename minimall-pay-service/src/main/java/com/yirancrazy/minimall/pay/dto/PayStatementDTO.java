package com.yirancrazy.minimall.pay.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayStatementDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class PayStatementDTO {


    @NotNull(message = "startDate cannot be null")
    private LocalDate startDate;


    @NotNull(message = "endDate cannot be null")
    private LocalDate endDate;
}
