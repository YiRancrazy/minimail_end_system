package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;

@Manager
public class PayManagerImpl extends ServiceImpl<PayTransactionMapper, PayTransactionPO>
    implements PayManager {
}