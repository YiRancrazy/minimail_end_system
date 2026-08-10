package com.yirancrazy.minimall.notify.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价统计视图，包含总数与各评分数量
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatsVO {

    /** 总评价数 */
    private long totalCount;

    /** 平均评分（保留两位小数） */
    private double avgRating;

    /** 5 分评价数 */
    private long rating5Count;

    /** 4 分评价数 */
    private long rating4Count;

    /** 3 分评价数 */
    private long rating3Count;

    /** 2 分评价数 */
    private long rating2Count;

    /** 1 分评价数 */
    private long rating1Count;
}
