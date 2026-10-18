package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaySnapshotDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaySnapshotDTO {

    private Long orderId;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    @Digits(integer = 10, fraction = 2, message = "payAmount must have at most 2 decimal places")
    private BigDecimal payAmount;
    private Integer status;
    private Long payId;
}
