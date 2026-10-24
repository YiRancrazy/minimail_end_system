package com.yirancrazy.minimall.order.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderCheckoutDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderCheckoutDTO {

    @NotNull(message = "结算来源不能为空")
    private CheckoutSourceEnum checkoutSource;

    @Size(max = 100, message = "单次最多结算100个购物车条目")
    private List<@NotNull(message = "购物车条目ID不能为空") Long> cartItemIds;

    @Valid
    private List<OrderCheckoutItemDTO> items;


    @Valid
    private ReceiverDTO receiver;
}
