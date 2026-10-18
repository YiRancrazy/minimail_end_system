package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentCreateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class CommentCreateDTO {


    @NotBlank(message = "orderNo cannot be blank")
    @Size(max = 64, message = "orderNo length must be <= 64")
    private String orderNo;


    @NotNull(message = "spuId cannot be null")
    private Long spuId;


    private Long skuId;


    private Long merchantId;


    @NotNull(message = "rating cannot be null")
    @Min(value = 1, message = "rating must be >= 1")
    @Max(value = 5, message = "rating must be <= 5")
    private Integer rating;


    @NotBlank(message = "content cannot be blank")
    @Size(max = 1024, message = "content length must be <= 1024")
    private String content;


    @Size(max = 4096, message = "images length must be <= 4096")
    private String images;


    private Integer anonymous;
}
