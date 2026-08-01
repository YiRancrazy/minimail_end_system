package com.yirancrazy.minimall.id.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.id.entity.IdSegmentPO;
import com.yirancrazy.minimall.id.manager.IdSegmentManager;
import com.yirancrazy.minimall.id.mapper.IdSegmentMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdSegment数据访问层实现，封装IdSegment表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Manager
public class IdSegmentManagerImpl extends ServiceImpl<IdSegmentMapper, IdSegmentPO>
    implements IdSegmentManager {
}