package com.yirancrazy.minimall.notify.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价 MyBatis Mapper 接口，映射 t_notify_comment 表
 * @Version: 1.1
 * @DateTime: 2026/08/10
 **/
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {

    /**
     * 按评分分组统计指定 SPU 的正常评价数量，聚合在数据库侧完成，避免全量加载内存累加。
     * @param spuId 商品 SPU ID
     * @param status 评价状态过滤（NORMAL）
     * @return 每行 {rating, cnt} 的分组结果
     */
    @Select("SELECT rating AS rating, COUNT(*) AS cnt FROM t_notify_comment "
        + "WHERE spu_id = #{spuId} AND status = #{status} AND is_deleted = 0 "
        + "GROUP BY rating")
    List<Map<String, Object>> countGroupByRating(@Param("spuId") Long spuId,
        @Param("status") Integer status);
}
