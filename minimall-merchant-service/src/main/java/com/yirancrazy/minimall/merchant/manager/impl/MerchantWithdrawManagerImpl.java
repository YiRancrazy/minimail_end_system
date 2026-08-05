package com.yirancrazy.minimall.merchant.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.merchant.mapper.MerchantWithdrawMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请 Manager 实现，封装 t_merchant_withdraw 表 CRUD。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Manager
public class MerchantWithdrawManagerImpl
    extends ServiceImpl<MerchantWithdrawMapper, MerchantWithdrawPO>
    implements MerchantWithdrawManager {
}
