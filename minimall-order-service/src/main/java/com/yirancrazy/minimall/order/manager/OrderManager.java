package com.yirancrazy.minimall.order.manager;

import java.time.LocalDateTime;
import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDTO;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDTO;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDTO;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OrderManager extends IService<OrderPO> {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    OrderStatisticsResultDTO statistics(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    List<OrderStatusCountDTO> countByStatus(Long userId);


    /**
     * 执行 summary 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    OrderSummaryResultDTO summary(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);


    /**
     * 执行 trend 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 结果列表
     */
    List<OrderTrendPointDTO> trend(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);
}
