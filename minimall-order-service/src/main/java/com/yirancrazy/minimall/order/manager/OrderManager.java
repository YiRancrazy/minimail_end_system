package com.yirancrazy.minimall.order.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.entity.OrderPO;

/**
* 订单数据访问管理接口，统一提供订单持久化实体的 MyBatis-Plus 通用数据操作能力。
 */
public interface OrderManager extends IService<OrderPO> {
}