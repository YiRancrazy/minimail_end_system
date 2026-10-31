package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.mapper.SpuMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 MyBatis-Plus ServiceImpl 提供 t_goods_spu 的通用持久化能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class SpuManagerImpl extends ServiceImpl<SpuMapper, SpuPo> implements SpuManager {}
