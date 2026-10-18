package com.yirancrazy.minimall.merchant.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.mapper.MerchantMapper;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class MerchantManagerImpl extends ServiceImpl<MerchantMapper, MerchantPO>
    implements MerchantManager {
}
