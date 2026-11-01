package com.yirancrazy.minimall.order.dto;

import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载用户结算请求，并区分购物车结算与立即购买。
 *   业务边界：checkoutSource 为必填；购物车结算使用 cartItemIds，立即购买使用 items，服务层校验二者互斥。
 *   使用场景：UserOrderControllerV1.checkout 接收并转交 OrderCheckoutService。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderCheckoutDto {

    @NotNull(message = "结算来源不能为空")
    private CheckoutSourceEnum checkoutSource;

    @Size(max = 100, message = "单次最多结算100个购物车条目")
    private List<@NotNull(message = "购物车条目ID不能为空") Long> cartItemIds;

    @Valid private List<OrderCheckoutItemDto> items;

    @Valid private ReceiverDto receiver;
}
