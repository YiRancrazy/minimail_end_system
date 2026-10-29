package com.yirancrazy.minimall.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算结果。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResultDTO {

    private Long orderId;

    private String orderGroupNo;

    private Long cartCleanupTaskId;
}
