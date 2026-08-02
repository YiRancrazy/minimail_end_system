package com.yirancrazy.minimall.order.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.entity.OutboxPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Outbox 数据访问层接口，封装 t_order_outbox 表 CRUD
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface OutboxManager extends IService<OutboxPO> {
}
