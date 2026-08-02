package com.yirancrazy.minimall.order.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.mapper.OutboxMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Outbox 数据访问层实现，封装 t_order_outbox 表 CRUD
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Manager
public class OutboxManagerImpl extends ServiceImpl<OutboxMapper, OutboxPO>
    implements OutboxManager {
}
