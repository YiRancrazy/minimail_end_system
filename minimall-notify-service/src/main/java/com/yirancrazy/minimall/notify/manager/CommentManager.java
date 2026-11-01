package com.yirancrazy.minimall.notify.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 t_notify_comment 的数据访问边界，在通用 MyBatis-Plus CRUD 之外提供评分分组统计查询.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CommentManager extends IService<CommentPo> {

    /**
     * 查询 SPU 下指定评价状态的评分分组计数.
     *
     * @param spuId 商品 SPU 编号
     * @param status 需要统计的评价状态编码
     * @return 各评分值对应的评价数量
     */
    List<CommentRatingCountDto> countGroupByRating(Long spuId, Integer status);
}
