package com.yirancrazy.minimall.api.dto.pay;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RefundCreateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
public class RefundCreateDTO {
    @NotNull(message = "payId cannot be null")
    private Long payId;

    @NotNull(message = "amount cannot be null")
    @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2, message = "amount must have at most 2 decimal places")
    private BigDecimal amount;

    private String reason;

    @NotBlank(message = "refundRequestNo cannot be blank")
    private String refundRequestNo;


    /**
     * 构造 RefundCreateDTO 实例。
     * @param payId payId 参数
     * @param amount amount 参数
     * @param reason reason 参数
     */
    public RefundCreateDTO(Long payId, BigDecimal amount, String reason) {
        this(payId, amount, reason, null);
    }


    /**
     * 构造 RefundCreateDTO 实例。
     * @param payId payId 参数
     * @param amount amount 参数
     * @param reason reason 参数
     * @param refundRequestNo refundRequestNo 参数
     */
    public RefundCreateDTO(Long payId, BigDecimal amount, String reason, String refundRequestNo) {
        this.payId = payId;
        this.amount = amount;
        this.reason = reason;
        this.refundRequestNo = refundRequestNo;
    }
}
