package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper;

/**
* 通知消息数据访问管理实现，基于 MyBatis-Plus ServiceImpl 完成对 t_notify_message 表的持久化。
 */
@Manager
public class NotifyManagerImpl extends ServiceImpl<NotifyMessageMapper, NotifyMessagePO>
    implements NotifyManager {
}