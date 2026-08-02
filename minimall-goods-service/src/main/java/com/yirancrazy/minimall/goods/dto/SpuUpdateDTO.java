package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu 修改入参，字段可空表示不更新对应列
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
public class SpuUpdateDTO {

    private Long categoryId;

    @Size(min = 1, max = 128, message = "title length must be between 1 and 128")
    private String title;

    @Size(max = 255, message = "subtitle length must be <= 255")
    private String subtitle;

    @Size(max = 255, message = "mainImageUrl length must be <= 255")
    private String mainImageUrl;
}
