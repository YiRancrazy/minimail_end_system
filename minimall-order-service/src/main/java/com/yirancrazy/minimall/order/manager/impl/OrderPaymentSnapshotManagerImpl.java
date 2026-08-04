package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderPaymentSnapshotPO;
import com.yirancrazy.minimall.order.manager.OrderPaymentSnapshotManager;
import com.yirancrazy.minimall.order.mapper.OrderPaymentSnapshotMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaymentSnapshot数据访问层实现，封装订单支付快照表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Manager
public class OrderPaymentSnapshotManagerImpl
    extends ServiceImpl<OrderPaymentSnapshotMapper, OrderPaymentSnapshotPO>
    implements OrderPaymentSnapshotManager {
}
