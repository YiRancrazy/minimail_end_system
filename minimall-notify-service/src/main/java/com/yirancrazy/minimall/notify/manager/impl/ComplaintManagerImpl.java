package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.mapper.ComplaintMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 ComplaintMapper 提供投诉记录的 MyBatis-Plus CRUD，业务状态流转由 ComplaintServiceImpl 控制.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class ComplaintManagerImpl extends ServiceImpl<ComplaintMapper, ComplaintPo>
    implements ComplaintManager {}
