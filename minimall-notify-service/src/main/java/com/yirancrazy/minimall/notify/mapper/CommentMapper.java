package com.yirancrazy.minimall.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供 t_notify_comment 的基础持久化能力，并查询指定 SPU、指定状态下各评分值的评价数量.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentPo> {

    /**
     * 按评分值聚合 SPU 下指定状态的未删除评价数量.
     *
     * @param spuId 商品 SPU 编号
     * @param status 需要统计的评价状态编码
     * @return 每个存在评价的评分值及其数量
     */
    @Select(
            "SELECT rating AS rating, COUNT(*) AS cnt FROM t_notify_comment "
                    + "WHERE spu_id = #{spuId} AND status = #{status} AND is_deleted = 0 "
                    + "GROUP BY rating")
    List<CommentRatingCountDto> countGroupByRating(
            @Param("spuId") Long spuId, @Param("status") Integer status);
}
