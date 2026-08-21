package com.yirancrazy.minimall.goods.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu 修改入参，字段可空表示不更新对应列；skus 非空时整表同步
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Data
public class SpuUpdateDTO {

    /** 归属店铺ID，传值则校验并变更店铺；不传保持原店铺 */
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

    /** 编辑后 SKU 全量清单，非空时按 id 增改、未出现者删除；空/未传不触碰 SKU */
    @Valid
    private List<SkuItemDTO> skus;
}
