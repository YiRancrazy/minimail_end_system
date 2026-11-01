package com.yirancrazy.minimall.notify.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 表示单个商品 SPU 的有效评价统计，包含总数、平均分和 1 至 5 分各档数量.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatsVo {

    private long totalCount;

    private double avgRating;

    private long rating5Count;

    private long rating4Count;

    private long rating3Count;

    private long rating2Count;

    private long rating1Count;
}
