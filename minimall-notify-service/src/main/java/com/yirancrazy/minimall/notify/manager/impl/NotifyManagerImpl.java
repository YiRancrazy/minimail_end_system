package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 NotifyMessageMapper 提供站内通知的 MyBatis-Plus CRUD，业务编排由 NotifyServiceImpl 负责.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class NotifyManagerImpl extends ServiceImpl<NotifyMessageMapper, NotifyMessagePo>
    implements NotifyManager {}
