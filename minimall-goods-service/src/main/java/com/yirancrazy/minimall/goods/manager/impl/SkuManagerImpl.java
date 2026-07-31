package com.yirancrazy.minimall.goods.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.mapper.SkuMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuManagerImpl description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class SkuManagerImpl extends ServiceImpl<SkuMapper, SkuPO>
    implements SkuManager {
}