package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.mapper.SpuMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class SpuManagerImpl extends ServiceImpl<SpuMapper, SpuPO>
    implements SpuManager {
}
