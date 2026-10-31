package com.yirancrazy.minimall.goods.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载用户端商品游标分页的关键字、类目和游标参数。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsPageDto extends CursorPageDto {

  private String keyword; // 查询关键字
  private Long categoryId; // 查询分类ID
}
