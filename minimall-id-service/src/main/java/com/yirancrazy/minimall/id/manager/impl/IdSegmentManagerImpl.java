package com.yirancrazy.minimall.id.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.id.entity.IdSegmentPo;
import com.yirancrazy.minimall.id.manager.IdSegmentManager;
import com.yirancrazy.minimall.id.mapper.IdSegmentMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 由 Spring 管理的 IdSegmentManager 实现。通过继承 ServiceImpl 复用
 *     IdSegmentMapper 的 t_id_segment CRUD；当前 IdService 未调用该实现。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class IdSegmentManagerImpl extends ServiceImpl<IdSegmentMapper, IdSegmentPo>
    implements IdSegmentManager {}
