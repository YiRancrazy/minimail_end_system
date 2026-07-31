package com.yirancrazy.minimall.order.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.mapper.OrderMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderManagerImpl description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class OrderManagerImpl extends ServiceImpl<OrderMapper, OrderPO>
    implements OrderManager {
}