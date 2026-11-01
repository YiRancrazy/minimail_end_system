package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.mapper.StockCountTaskMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 MyBatis-Plus 和条件更新 SQL 实现盘点任务状态迁移。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class StockCountTaskManagerImpl extends ServiceImpl<StockCountTaskMapper, StockCountTaskPo>
        implements StockCountTaskManager {

    /**
     * 委托 Mapper 以状态条件完成盘点，返回受影响行数以识别并发竞争。
     *
     * @param id 盘点任务 ID
     * @param fromStatus 更新前必须匹配的状态码
     * @param toStatus 完成后写入的状态码
     * @param actualQuantity 实盘数量
     * @param diffQuantity 实盘与账面数量之差
     * @param remark 盘点备注
     * @return 实际更新行数；0 表示任务不存在或状态已变化
     */
    @Override
    public int completeIfStatus(
            Long id,
            int fromStatus,
            int toStatus,
            Long actualQuantity,
            Long diffQuantity,
            String remark) {
        return baseMapper.completeIfStatus(
                id, fromStatus, toStatus, actualQuantity, diffQuantity, remark);
    }

    /**
     * 委托 Mapper 以状态条件取消盘点，避免完成与取消并发覆盖。
     *
     * @param id 盘点任务 ID
     * @param fromStatus 取消前必须匹配的状态码
     * @param toStatus 取消后写入的状态码
     * @return 实际更新行数；0 表示任务不存在或状态已变化
     */
    @Override
    public int cancelIfStatus(Long id, int fromStatus, int toStatus) {
        return baseMapper.cancelIfStatus(id, fromStatus, toStatus);
    }
}
