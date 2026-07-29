package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.PayRecordPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayRecordMapper;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付单数据访问层管理实现，基于 MyBatis-Plus 的 ServiceImpl 承载 t_pay_record 表的 CRUD。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
@Manager
public class PayManagerImpl extends ServiceImpl<PayRecordMapper, PayRecordPO>
    implements PayManager {
}