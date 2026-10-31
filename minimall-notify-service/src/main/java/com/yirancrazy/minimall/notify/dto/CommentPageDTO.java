package com.yirancrazy.minimall.notify.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载评价游标分页条件和 SPU、用户、商家、订单号、评价状态筛选条件，供公开、用户、商家及平台评价查询复用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentPageDto extends CursorPageDto {

  private Long spuId;

  private Long userId;

  private Long merchantId;

  private String orderNo;

  private Integer status;
}
