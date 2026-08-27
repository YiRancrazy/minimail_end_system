package com.yirancrazy.minimall.order.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车结算DTO，支持多SKU批量下单。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Data
public class OrderCheckoutDTO {

    @NotEmpty(message = "结算商品列表不能为空")
    @Valid
    private List<OrderCheckoutItemDTO> items;

    /** 收货人信息，序列化为快照落库；未传则不记录收货信息 */
    @Valid
    private ReceiverDTO receiver;
}
