package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Sku 分页查询入参，支持按名称模糊搜索
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class SkuPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private String skuName;
}
