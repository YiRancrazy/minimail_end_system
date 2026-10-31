package com.yirancrazy.minimall.notify.dto;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射评价评分分组统计结果，保存单个评分值及其有效评价数量.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CommentRatingCountDto {

  private Integer rating;

  private Long cnt;
}
