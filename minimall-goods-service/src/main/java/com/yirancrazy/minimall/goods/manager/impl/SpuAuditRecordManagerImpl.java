package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.mapper.SpuAuditRecordMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuAuditRecordManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class SpuAuditRecordManagerImpl extends ServiceImpl<SpuAuditRecordMapper, SpuAuditRecordPO>
    implements SpuAuditRecordManager {
}
