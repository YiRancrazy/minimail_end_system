package com.yirancrazy.minimall.order.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderCheckoutDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderCheckoutDTO {

    @NotEmpty(message = "结算商品列表不能为空")
    @Valid
    private List<OrderCheckoutItemDTO> items;


    @Valid
    private ReceiverDTO receiver;
}
