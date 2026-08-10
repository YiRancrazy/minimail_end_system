package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价创建 DTO，用户提交评价入参
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class CommentCreateDTO {

    /** 关联订单号 */
    @NotBlank(message = "orderNo cannot be blank")
    @Size(max = 64, message = "orderNo length must be <= 64")
    private String orderNo;

    /** 商品 SPU ID */
    @NotNull(message = "spuId cannot be null")
    private Long spuId;

    /** 商品 SKU ID，可空 */
    private Long skuId;

    /** 商家ID，可选；缺失时服务端按 spuId 查询补齐 */
    private Long merchantId;

    /** 评分 1-5 */
    @NotNull(message = "rating cannot be null")
    @Min(value = 1, message = "rating must be >= 1")
    @Max(value = 5, message = "rating must be <= 5")
    private Integer rating;

    /** 评价内容 */
    @NotBlank(message = "content cannot be blank")
    @Size(max = 1024, message = "content length must be <= 1024")
    private String content;

    /** 评价图片，逗号分隔 objectKey 列表 */
    @Size(max = 4096, message = "images length must be <= 4096")
    private String images;

    /** 是否匿名 0=否 1=是 */
    private Integer anonymous;
}
