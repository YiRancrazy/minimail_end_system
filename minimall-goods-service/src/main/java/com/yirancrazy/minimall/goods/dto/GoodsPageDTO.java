package com.yirancrazy.minimall.goods.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品分页查询入参，仅检索在售商品，支持关键词与分类过滤
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsPageDTO extends CursorPageDTO {

    private String keyword;      // 查询关键字
    private Long categoryId;     // 查询分类ID
}
