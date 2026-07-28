package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.mapper.SkuMapper;
import org.springframework.stereotype.Service;

@Service
@Manager
public class SkuManagerImpl extends ServiceImpl<SkuMapper, SkuPO>
    implements SkuManager {
}