package com.yirancrazy.minimall.goods.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载 SPU 游标分页的商家、状态、标题和游标过滤条件。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpuPageDto extends CursorPageDto {

  private Long merchantId;
  private Integer status;
  private String title;
}
