package com.yirancrazy.minimall.stock.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.manager.StockCountTaskManager;
import com.yirancrazy.minimall.stock.mapper.StockCountTaskMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class StockCountTaskManagerImpl extends ServiceImpl<StockCountTaskMapper, StockCountTaskPO>
    implements StockCountTaskManager {

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
    @Override
    public int completeIfStatus(Long id, int fromStatus, int toStatus, Long actualQuantity,
                                Long diffQuantity, String remark) {
        return baseMapper.completeIfStatus(id, fromStatus, toStatus, actualQuantity,
            diffQuantity, remark);
    }

    /**
     * 执行 cancelIfStatus 方法。
     * @param id id 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    @Override
    public int cancelIfStatus(Long id, int fromStatus, int toStatus) {
        return baseMapper.cancelIfStatus(id, fromStatus, toStatus);
    }
}
