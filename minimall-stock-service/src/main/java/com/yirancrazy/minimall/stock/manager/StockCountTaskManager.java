package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装盘点任务持久化访问，并提供带前置状态校验的条件更新。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockCountTaskManager extends IService<StockCountTaskPo> {

    /**
     * 仅当盘点任务仍处于指定前置状态时，原子更新为完成状态并写入实盘结果。
     *
     * @param id 盘点任务 ID
     * @param fromStatus 更新前必须匹配的状态码
     * @param toStatus 完成后写入的状态码
     * @param actualQuantity 实盘数量
     * @param diffQuantity 实盘数量与账面数量之差
     * @param remark 盘点备注
     * @return 实际更新的行数；0 表示任务不存在或状态已被并发修改
     */
    int completeIfStatus(
            Long id, int fromStatus, int toStatus, Long actualQuantity, Long diffQuantity, String remark);

    /**
     * 仅当盘点任务仍处于指定前置状态时，原子迁移到取消状态。
     *
     * @param id 盘点任务 ID
     * @param fromStatus 取消前必须匹配的状态码
     * @param toStatus 取消后写入的状态码
     * @return 实际更新的行数；0 表示任务不存在或状态已被并发修改
     */
    int cancelIfStatus(Long id, int fromStatus, int toStatus);
}
