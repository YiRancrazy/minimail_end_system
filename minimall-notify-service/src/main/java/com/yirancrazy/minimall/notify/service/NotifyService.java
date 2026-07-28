package com.yirancrazy.minimall.notify.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotifyService {

    private final NotifyManager notifyManager;

    public NotifyService(NotifyManager notifyManager) {
        this.notifyManager = notifyManager;
    }

    public boolean push(Long userId, String title, String content) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setReadFlag(0);
        notifyManager.save(m);
        return true;
    }

    public List<NotifyMessagePO> listByUser(Long userId) {
        return notifyManager.list(Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getUserId, userId));
    }
}