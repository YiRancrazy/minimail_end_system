package com.yirancrazy.minimall.id.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.id.entity.IdSegmentPO;
import com.yirancrazy.minimall.id.manager.IdSegmentManager;
import com.yirancrazy.minimall.id.mapper.IdSegmentMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 号段数据访问管理实现，基于 MyBatis-Plus 的 ServiceImpl 提供对 id_segment 表的持久化能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Manager
public class IdSegmentManagerImpl extends ServiceImpl<IdSegmentMapper, IdSegmentPO>
    implements IdSegmentManager {
}