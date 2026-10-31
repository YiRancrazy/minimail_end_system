package com.yirancrazy.minimall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 访问 t_stock_count_task，并通过条件 SQL 原子迁移盘点任务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface StockCountTaskMapper extends BaseMapper<StockCountTaskPo> {

  /**
   * 在任务状态匹配时写入实盘数量、差异、备注并更新完成时间。
   *
   * @param id 盘点任务 ID
   * @param fromStatus 更新前必须匹配的状态码
   * @param toStatus 完成后写入的状态码
   * @param actualQuantity 实盘数量
   * @param diffQuantity 实盘与账面数量之差
   * @param remark 盘点备注
   * @return 实际更新行数；0 表示任务不存在或状态不匹配
   */
  @Update(
      "UPDATE t_stock_count_task SET status = #{toStatus}, "
          + "actual_quantity = #{actualQuantity}, diff_quantity = #{diffQuantity}, "
          + "remark = #{remark}, update_time = NOW() "
          + "WHERE id = #{id} AND status = #{fromStatus}")
  int completeIfStatus(
      @Param("id") Long id,
      @Param("fromStatus") int fromStatus,
      @Param("toStatus") int toStatus,
      @Param("actualQuantity") Long actualQuantity,
      @Param("diffQuantity") Long diffQuantity,
      @Param("remark") String remark);

  /**
   * 在任务状态匹配时仅更新状态和更新时间，不修改盘点数量字段。
   *
   * @param id 盘点任务 ID
   * @param fromStatus 更新前必须匹配的状态码
   * @param toStatus 取消后写入的状态码
   * @return 实际更新行数；0 表示任务不存在或状态不匹配
   */
  @Update(
      "UPDATE t_stock_count_task SET status = #{toStatus}, update_time = NOW() "
          + "WHERE id = #{id} AND status = #{fromStatus}")
  int cancelIfStatus(
      @Param("id") Long id, @Param("fromStatus") int fromStatus, @Param("toStatus") int toStatus);
}
