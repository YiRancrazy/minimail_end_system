package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.order.dto.OrderCheckoutDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算编排服务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
public interface OrderCheckoutService {

    /**
     * 创建结算订单并触发购物车清理。
     * @param userId 用户 ID
     * @param dto 结算参数
     * @return 主订单 ID
     */
    Long checkout(Long userId, OrderCheckoutDTO dto);
}
