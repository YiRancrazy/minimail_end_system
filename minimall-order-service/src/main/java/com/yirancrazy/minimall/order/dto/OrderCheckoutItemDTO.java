package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 描述一次结算中的 SKU 与购买数量。
 *   业务边界：仅表达请求明细，商品名称、价格和商家信息由订单服务读取服务端快照补齐。
 *   使用场景：立即购买请求及多 SKU 结算输入使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckoutItemDto {

    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;
}
