package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.mapper.SpuAuditRecordMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品审核记录数据访问层实现，封装 t_goods_audit_record 表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Manager
public class SpuAuditRecordManagerImpl extends ServiceImpl<SpuAuditRecordMapper, SpuAuditRecordPO>
    implements SpuAuditRecordManager {
}
