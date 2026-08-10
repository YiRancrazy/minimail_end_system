package com.yirancrazy.minimall.order.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家发货入参，承运商与运单号。
 * @Version: 1.0
 * @DateTime: 2026/08/09
 **/
@Data
public class OrderShipDTO {

    @NotBlank(message = "承运商不能为空")
    private String carrier;

    /** 6-32 位字母数字。 */
    @NotBlank(message = "运单号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{6,32}$", message = "运单号格式不正确")
    private String trackingNo;

    /** 可选，运费；与金额计算无关，结算使用。 */
    private BigDecimal freight;
}