package com.yirancrazy.minimall.goods.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家修改 SPU 展示信息、所属店铺及 SKU 明细的增量字段。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SpuUpdateDto {

    private Long shopId;

    private Long categoryId;

    @Size(min = 1, max = 128, message = "title length must be between 1 and 128")
    private String title;

    @Size(max = 255, message = "subtitle length must be <= 255")
    private String subtitle;

    // 兼容前端字段名 mainImage，两者均映射主图 URL 列
    @JsonAlias("mainImage")
    @Size(max = 255, message = "mainImageUrl length must be <= 255")
    private String mainImageUrl;

    @Valid private List<SkuItemDto> skus;
}
