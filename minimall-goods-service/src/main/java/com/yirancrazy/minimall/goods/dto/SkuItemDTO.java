package com.yirancrazy.minimall.goods.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SPU 创建/更新入参中携带的 SKU 子项，前端以 spec 字段表达规格描述
 * @Version: 1.0
 * @DateTime: 2026/08/21
 */
@Data
public class SkuItemDTO {

    /** 编辑场景下已存在 SKU 的主键，新建时为空 */
    private Long id;

    // 兼容前端字段名 spec，即规格描述，落库为 skuName
    @JsonAlias("spec")
    @Size(max = 200, message = "skuName length must be <= 200")
    private String skuName;

    @Min(value = 0, message = "price must be non-negative")
    private BigDecimal price;

    @Min(value = 0, message = "stock must be non-negative")
    private Integer stock;
}
