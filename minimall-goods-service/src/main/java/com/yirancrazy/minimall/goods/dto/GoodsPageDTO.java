package com.yirancrazy.minimall.goods.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsPageDTO extends CursorPageDTO {

    private String keyword;      // 查询关键字
    private Long categoryId;     // 查询分类ID
}
