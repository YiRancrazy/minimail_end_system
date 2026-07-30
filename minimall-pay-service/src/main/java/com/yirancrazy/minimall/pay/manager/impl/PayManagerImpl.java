package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据访问层实现，继承 ServiceImpl，提供基础 CRUD 操作。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Manager
public class PayManagerImpl extends ServiceImpl<PayTransactionMapper, PayTransactionPO>
    implements PayManager {
}