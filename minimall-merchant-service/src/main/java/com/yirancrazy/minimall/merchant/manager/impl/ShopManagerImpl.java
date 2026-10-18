package com.yirancrazy.minimall.merchant.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.mapper.ShopMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class ShopManagerImpl extends ServiceImpl<ShopMapper, ShopPO>
    implements ShopManager {
}