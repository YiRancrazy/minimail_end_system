package com.yirancrazy.minimall.pay.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.pay.entity.PayRecordPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付单数据访问层管理接口，继承 MP 的 IService 复用通用 CRUD，供 Service 层调用。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface PayManager extends IService<PayRecordPO> {
}