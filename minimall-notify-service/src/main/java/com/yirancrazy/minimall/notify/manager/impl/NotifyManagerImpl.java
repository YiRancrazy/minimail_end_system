package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.mapper.NotifyMessageMapper;
import org.springframework.stereotype.Service;

@Service
@Manager
public class NotifyManagerImpl extends ServiceImpl<NotifyMessageMapper, NotifyMessagePO>
    implements NotifyManager {
}