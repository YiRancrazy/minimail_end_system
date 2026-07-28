package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayRecordPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRecordMapper;
import org.springframework.stereotype.Service;

@Service
@Manager
public class PayManagerImpl extends ServiceImpl<PayRecordMapper, PayRecordPO>
    implements PayManager {
}