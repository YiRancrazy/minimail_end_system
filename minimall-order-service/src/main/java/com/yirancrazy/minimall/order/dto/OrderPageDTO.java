package com.yirancrazy.minimall.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单分页查询入参，支持按用户/商家/状态过滤
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
public class OrderPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private Long userId;
    private Long merchantId;
    private Integer status;
}
