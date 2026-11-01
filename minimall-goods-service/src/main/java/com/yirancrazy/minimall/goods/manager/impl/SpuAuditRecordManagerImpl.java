package com.yirancrazy.minimall.goods.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPo;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.mapper.SpuAuditRecordMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 MyBatis-Plus ServiceImpl 提供商品审核记录的通用持久化能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class SpuAuditRecordManagerImpl extends ServiceImpl<SpuAuditRecordMapper, SpuAuditRecordPo>
        implements SpuAuditRecordManager {}
