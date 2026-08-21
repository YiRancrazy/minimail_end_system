package com.yirancrazy.minimall.goods.dto;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu 创建入参，商家发布新商品 SPU 时提交，可携带 SKU 清单一并落库
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Data
public class SpuCreateDTO {

    /** 归属店铺ID，服务端校验店铺存在、归属当前商家且状态为营业中 */
    @NotNull(message = "shopId cannot be null")
    private Long shopId;

    @NotNull(message = "categoryId cannot be null")
    private Long categoryId;

    @NotBlank(message = "title cannot be blank")
    @Size(min = 1, max = 128, message = "title length must be between 1 and 128")
    private String title;

    @Size(max = 255, message = "subtitle length must be <= 255")
    private String subtitle;

    // 兼容前端字段名 mainImage，两者均映射主图 URL 列
    @JsonAlias("mainImage")
    @Size(max = 255, message = "mainImageUrl length must be <= 255")
    private String mainImageUrl;

    /** 创建时携带的 SKU 清单，可为空；spuId 由服务端回填 */
    @Valid
    private List<SkuItemDTO> skus = new ArrayList<>();
}
