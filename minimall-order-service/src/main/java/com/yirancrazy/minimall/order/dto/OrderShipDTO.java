package com.yirancrazy.minimall.order.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderShipDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderShipDTO {

    @NotBlank(message = "承运商不能为空")
    private String carrier;


    @NotBlank(message = "运单号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{6,32}$", message = "运单号格式不正确")
    private String trackingNo;


    @Digits(integer = 10, fraction = 2, message = "运费最多10位整数且最多2位小数")
    private BigDecimal freight;
}
