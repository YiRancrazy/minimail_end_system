package com.yirancrazy.minimall.goods.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Sku 分页查询入参，支持按名称模糊搜索
 * @Version: 2.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SkuPageDTO extends CursorPageDTO {

    private String skuName;
}
