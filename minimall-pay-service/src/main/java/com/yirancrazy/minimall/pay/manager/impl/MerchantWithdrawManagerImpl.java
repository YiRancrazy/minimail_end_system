package com.yirancrazy.minimall.pay.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.pay.mapper.MerchantWithdrawMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现数据访问层实现，继承 ServiceImpl，提供基础 CRUD 操作。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Manager
public class MerchantWithdrawManagerImpl
    extends ServiceImpl<MerchantWithdrawMapper, MerchantWithdrawPO>
    implements MerchantWithdrawManager {
}
