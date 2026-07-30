package com.yirancrazy.minimall.merchant.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.mapper.ShopMapper;

/**
* 店铺数据访问管理实现，基于 MyBatis-Plus ServiceImpl 承载店铺表通用 CRUD 操作。
 */
@Service
@Manager
public class ShopManagerImpl extends ServiceImpl<ShopMapper, ShopPO>
    implements ShopManager {
}