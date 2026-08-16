package com.yirancrazy.minimall.api.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单导出项跨服务 DTO，供平台端订单导出 CSV 使用。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExportItemDTO {

    private Long id;
    private Long userId;
    private Long merchantId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
}
