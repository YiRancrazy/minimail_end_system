package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTask Mapper接口，提供t_stock_count_task表的基础CRUD操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface StockCountTaskMapper extends BaseMapper<StockCountTaskPO> {

    /**
     * 条件完成盘点任务：仅当任务处于 fromStatus 时置为 toStatus 并落盘点结果字段，返回影响行数。
     * 条件内嵌状态校验保证并发完成请求只有一个命中，未命中（0 行）表示任务已被并发方处理，
     * 调用方不得再次入账盘点差异。
     * @param id 任务ID
     * @param fromStatus 期望的当前状态
     * @param toStatus 目标状态
     * @param actualQuantity 实际盘点数量
     * @param diffQuantity 盘点差异数量（actual - expected）
     * @param remark 备注
     * @return 影响行数，0 表示任务非 fromStatus 状态
     */
    @Update("UPDATE t_stock_count_task SET status = #{toStatus}, "
        + "actual_quantity = #{actualQuantity}, diff_quantity = #{diffQuantity}, "
        + "remark = #{remark}, update_time = NOW() "
        + "WHERE id = #{id} AND status = #{fromStatus}")
    int completeIfStatus(@Param("id") Long id, @Param("fromStatus") int fromStatus,
                         @Param("toStatus") int toStatus, @Param("actualQuantity") Long actualQuantity,
                         @Param("diffQuantity") Long diffQuantity, @Param("remark") String remark);

    /**
     * 条件取消盘点任务：仅当任务处于 fromStatus 时推进到 toStatus，返回影响行数。
     * 与完成操作同理，保证并发场景下取消/完成互斥，只允许一个请求生效。
     * @param id 任务ID
     * @param fromStatus 期望的当前状态
     * @param toStatus 目标状态
     * @return 影响行数，0 表示任务非 fromStatus 状态
     */
    @Update("UPDATE t_stock_count_task SET status = #{toStatus}, update_time = NOW() "
        + "WHERE id = #{id} AND status = #{fromStatus}")
    int cancelIfStatus(@Param("id") Long id, @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus);
}
