package com.yirancrazy.minimall.notify.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentStatsVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentStatsVO {


    private long totalCount;


    private double avgRating;


    private long rating5Count;


    private long rating4Count;


    private long rating3Count;


    private long rating2Count;


    private long rating1Count;
}
