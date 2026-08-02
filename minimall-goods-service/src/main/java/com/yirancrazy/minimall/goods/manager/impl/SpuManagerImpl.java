package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.mapper.SpuMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu数据访问层实现，封装t_goods_spu表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Manager
public class SpuManagerImpl extends ServiceImpl<SpuMapper, SpuPO>
    implements SpuManager {
}
