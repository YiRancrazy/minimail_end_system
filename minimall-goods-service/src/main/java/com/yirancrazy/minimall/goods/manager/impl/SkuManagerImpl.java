package com.yirancrazy.minimall.goods.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.mapper.SkuMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Sku数据访问层实现，封装Sku表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Manager
public class SkuManagerImpl extends ServiceImpl<SkuMapper, SkuPO>
    implements SkuManager {
}