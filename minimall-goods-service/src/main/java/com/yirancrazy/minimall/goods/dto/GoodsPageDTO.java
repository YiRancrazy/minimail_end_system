package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品分页查询入参，仅检索在售商品，支持关键词与分类过滤
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
public class GoodsPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    private String keyword;

    private Long categoryId;
}
