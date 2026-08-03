package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品搜索 DTO，支持关键词、分类、价格范围过滤
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class SpuSearchDTO {

    /** 搜索关键词，null 或空时不做全文检索 */
    private String keyword;

    /** 分类ID，null 不限制 */
    private Long categoryId;

    /** 最低价格（分），null 不限制 */
    private Long minPrice;

    /** 最高价格（分），null 不限制 */
    private Long maxPrice;

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;
}
