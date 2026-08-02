package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RefundCreate 数据传输对象，跨服务退款创建入参
 * @Version: 1.1
 * @DateTime: 2026/08/02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundCreateDTO {
    @NotNull(message = "payId cannot be null")
    private Long payId;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    private BigDecimal amount;

    private String reason;
}
