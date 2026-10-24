package com.yirancrazy.minimall.order.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车结算清理任务服务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
public interface CartCleanupService {

    /**
     * 尝试执行单个清理任务。
     * @param taskId 任务 ID
     * @return 是否清理成功
     */
    boolean tryCleanup(Long taskId);

    /**
     * 扫描并执行到期的清理任务。
     * @return 成功清理数量
     */
    int scanPending();
}
