package com.yirancrazy.minimall.stock.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface StockCountTaskManager extends IService<StockCountTaskPO> {


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
    int completeIfStatus(Long id, int fromStatus, int toStatus, Long actualQuantity,
                         Long diffQuantity, String remark);


    /**
     * 执行 cancelIfStatus 方法。
     * @param id id 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    int cancelIfStatus(Long id, int fromStatus, int toStatus);
}
