package com.yirancrazy.minimall.merchant.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.merchant.entity.ShopPo;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.mapper.ShopMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺 Manager 实现；通过 ShopMapper 复用 MyBatis-Plus 基础 CRUD，并由 @Manager 注册为 Spring Bean。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class ShopManagerImpl extends ServiceImpl<ShopMapper, ShopPo> implements ShopManager {}
