package com.yirancrazy.minimall.id.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.id.entity.IdSegmentPO;
import com.yirancrazy.minimall.id.manager.IdSegmentManager;
import com.yirancrazy.minimall.id.mapper.IdSegmentMapper;

@Manager
public class IdSegmentManagerImpl extends ServiceImpl<IdSegmentMapper, IdSegmentPO>
    implements IdSegmentManager {
}