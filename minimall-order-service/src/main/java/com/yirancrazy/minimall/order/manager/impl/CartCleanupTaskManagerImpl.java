package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPo;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.mapper.CartCleanupTaskMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现 CartCleanupTaskManager，通过 MyBatis-Plus 访问CartCleanupTaskPo。
 *   业务边界：作为 @Manager Bean 只处理持久化细节，不承载跨服务事务和业务状态机。
 *   使用场景：由订单服务或补偿任务依赖 base 接口调用。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Manager
public class CartCleanupTaskManagerImpl
        extends ServiceImpl<CartCleanupTaskMapper, CartCleanupTaskPo>
        implements CartCleanupTaskManager {}
