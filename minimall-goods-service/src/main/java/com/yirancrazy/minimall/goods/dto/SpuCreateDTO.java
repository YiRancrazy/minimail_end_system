package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu 创建入参，商家发布新商品 SPU 时提交
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class SpuCreateDTO {

    @NotNull(message = "categoryId cannot be null")
    private Long categoryId;

    @NotBlank(message = "title cannot be blank")
    @Size(min = 1, max = 128, message = "title length must be between 1 and 128")
    private String title;

    @Size(max = 255, message = "subtitle length must be <= 255")
    private String subtitle;

    @Size(max = 255, message = "mainImageUrl length must be <= 255")
    private String mainImageUrl;
}
