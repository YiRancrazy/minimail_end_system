package com.yirancrazy.minimall.merchant.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.mapper.ShopMapper;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺数据访问管理实现，基于 MyBatis-Plus ServiceImpl 承载店铺表通用 CRUD 操作。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
@Manager
public class ShopManagerImpl extends ServiceImpl<ShopMapper, ShopPO>
    implements ShopManager {
}