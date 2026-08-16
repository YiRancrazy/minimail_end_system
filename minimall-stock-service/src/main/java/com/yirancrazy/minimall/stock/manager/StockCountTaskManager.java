package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTask数据访问层接口，定义t_stock_count_task表操作契约
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
public interface StockCountTaskManager extends IService<StockCountTaskPO> {

    /**
     * 条件完成盘点任务：仅当任务处于 fromStatus 时推进到 toStatus 并落盘点结果字段。
     * @param id 任务ID
     * @param fromStatus 期望的当前状态
     * @param toStatus 目标状态
     * @param actualQuantity 实际盘点数量
     * @param diffQuantity 盘点差异数量
     * @param remark 备注
     * @return 影响行数，0 表示任务已被并发方处理
     */
    int completeIfStatus(Long id, int fromStatus, int toStatus, Long actualQuantity,
                         Long diffQuantity, String remark);

    /**
     * 条件取消盘点任务：仅当任务处于 fromStatus 时推进到 toStatus。
     * @param id 任务ID
     * @param fromStatus 期望的当前状态
     * @param toStatus 目标状态
     * @return 影响行数，0 表示任务已被并发方处理
     */
    int cancelIfStatus(Long id, int fromStatus, int toStatus);
}
