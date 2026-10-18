package com.yirancrazy.minimall.notify.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {


    /**
     * 统计业务数据。
     * @param spuId spuId 参数
     * @param status status 参数
     * @return 结果列表
     */
    @Select("SELECT rating AS rating, COUNT(*) AS cnt FROM t_notify_comment "
        + "WHERE spu_id = #{spuId} AND status = #{status} AND is_deleted = 0 "
        + "GROUP BY rating")
    List<CommentRatingCountDTO> countGroupByRating(@Param("spuId") Long spuId,
        @Param("status") Integer status);
}
