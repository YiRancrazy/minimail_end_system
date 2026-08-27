package com.yirancrazy.minimall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据传输对象，用于接收请求参数。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    @NotNull(message = "商品SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    /** 收货人信息，序列化为快照落库；未传则不记录收货信息 */
    @Valid
    private ReceiverDTO receiver;
}