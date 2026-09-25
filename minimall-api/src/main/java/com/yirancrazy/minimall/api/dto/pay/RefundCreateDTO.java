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
 * @Description: RefundCreate 数据传输对象，跨服务退款创建入参
 * @Version: 1.1
 * @DateTime: 2026/08/02
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
     * 兼容存量调用，保留无幂等键构造器。
     * @param payId 支付流水ID
     * @param amount 退款金额
     * @param reason 退款原因
     */
    public RefundCreateDTO(Long payId, BigDecimal amount, String reason) {
        this(payId, amount, reason, null);
    }

    /**
     * 创建退款请求。
     * @param payId 支付流水ID
     * @param amount 退款金额
     * @param reason 退款原因
     * @param refundRequestNo 退款请求幂等键
     */
    public RefundCreateDTO(Long payId, BigDecimal amount, String reason, String refundRequestNo) {
        this.payId = payId;
        this.amount = amount;
        this.reason = reason;
        this.refundRequestNo = refundRequestNo;
    }
}
