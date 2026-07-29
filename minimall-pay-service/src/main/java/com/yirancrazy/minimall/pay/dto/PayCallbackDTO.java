package com.yirancrazy.minimall.pay.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCallbackDTO {

    @NotNull(message = "支付ID不能为空")
    private Long payId;
}