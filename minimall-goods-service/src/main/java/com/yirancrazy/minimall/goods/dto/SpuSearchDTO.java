package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuSearchDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SpuSearchDTO {


    private String keyword;


    private Long categoryId;


    private Long minPrice;


    private Long maxPrice;

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;
}
