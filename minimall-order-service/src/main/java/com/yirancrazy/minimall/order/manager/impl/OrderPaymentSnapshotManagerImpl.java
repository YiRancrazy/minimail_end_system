package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OrderPaymentSnapshotPO;
import com.yirancrazy.minimall.order.manager.OrderPaymentSnapshotManager;
import com.yirancrazy.minimall.order.mapper.OrderPaymentSnapshotMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderPaymentSnapshotManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class OrderPaymentSnapshotManagerImpl
    extends ServiceImpl<OrderPaymentSnapshotMapper, OrderPaymentSnapshotPO>
    implements OrderPaymentSnapshotManager {
}
