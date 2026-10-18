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
 * @Description: SpuCreateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SpuCreateDTO {


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


    @Valid
    private List<SkuItemDTO> skus = new ArrayList<>();
}
