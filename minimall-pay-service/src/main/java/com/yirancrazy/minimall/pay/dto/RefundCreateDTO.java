package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RefundCreateDTO {
    @NotBlank(message = "paymentNo cannot be blank")
    private String paymentNo;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    private BigDecimal amount;

    private String reason;
}