package com.yirancrazy.minimall.merchant.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.mapper.ShopMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopManagerImpl description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class ShopManagerImpl extends ServiceImpl<ShopMapper, ShopPO>
    implements ShopManager {
}