package com.yirancrazy.minimall.merchant.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.mapper.MerchantMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家主体数据访问层实现，封装 t_merch_merchant 表 CRUD 操作。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Manager
public class MerchantManagerImpl extends ServiceImpl<MerchantMapper, MerchantPO>
    implements MerchantManager {
}
