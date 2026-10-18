package com.yirancrazy.minimall.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface StockCountTaskMapper extends BaseMapper<StockCountTaskPO> {


    /**
     * 执行 completeIfStatus 方法。
     * @param id id 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @param actualQuantity actualQuantity 参数
     * @param diffQuantity diffQuantity 参数
     * @param remark remark 参数
     * @return 业务数值
     */
    @Update("UPDATE t_stock_count_task SET status = #{toStatus}, "
        + "actual_quantity = #{actualQuantity}, diff_quantity = #{diffQuantity}, "
        + "remark = #{remark}, update_time = NOW() "
        + "WHERE id = #{id} AND status = #{fromStatus}")
    int completeIfStatus(@Param("id") Long id, @Param("fromStatus") int fromStatus,
                         @Param("toStatus") int toStatus, @Param("actualQuantity") Long actualQuantity,
                         @Param("diffQuantity") Long diffQuantity, @Param("remark") String remark);


    /**
     * 执行 cancelIfStatus 方法。
     * @param id id 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    @Update("UPDATE t_stock_count_task SET status = #{toStatus}, update_time = NOW() "
        + "WHERE id = #{id} AND status = #{fromStatus}")
    int cancelIfStatus(@Param("id") Long id, @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus);
}
