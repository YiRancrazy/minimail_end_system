package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.mapper.SkuMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 MyBatis-Plus ServiceImpl 提供 t_sku 的通用持久化能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class SkuManagerImpl extends ServiceImpl<SkuMapper, SkuPo> implements SkuManager {}
