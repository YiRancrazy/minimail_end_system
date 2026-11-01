package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家发货所需的承运商、运单号和可选运费。
 *   业务边界：字段级校验限制承运商非空、运单号格式及运费小数位；订单归属和状态迁移由服务层校验。
 *   使用场景：MerchantOrderControllerV1.ship 请求体使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderShipDto {

    @NotBlank(message = "承运商不能为空")
    private String carrier;

    @NotBlank(message = "运单号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9]{6,32}$", message = "运单号格式不正确")
    private String trackingNo;

    @Digits(integer = 10, fraction = 2, message = "运费最多10位整数且最多2位小数")
    private BigDecimal freight;
}
