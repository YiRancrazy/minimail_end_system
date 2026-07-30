package com.yirancrazy.minimall.goods.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.mapper.SkuMapper;

/**
* 商品数据访问管理实现，基于 MyBatis-Plus ServiceImpl 暴露 SKU 表的通用持久化能力。
 */
@Service
@Manager
public class SkuManagerImpl extends ServiceImpl<SkuMapper, SkuPO>
    implements SkuManager {
}