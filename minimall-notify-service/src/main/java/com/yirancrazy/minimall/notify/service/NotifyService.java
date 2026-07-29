package com.yirancrazy.minimall.notify.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知领域服务，封装消息持久化、按用户查询等业务逻辑并委托 Manager 落地。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
public class NotifyService {

    private final NotifyManager notifyManager;

    public NotifyService(NotifyManager notifyManager) {
        this.notifyManager = notifyManager;
    }

    /**
     * 构造通知实体并写入数据库，已读标记默认为未读。
     *
     * @param userId 接收用户主键
     * @param title 通知标题
     * @param content 通知正文
     * @return 入库是否成功
     */
    public boolean push(Long userId, String title, String content) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setReadFlag(0);
        notifyManager.save(m);
        return true;
    }

    /**
     * 查询指定用户全部通知消息，按持久化顺序返回列表。
     *
     * @param userId 目标用户主键
     * @return 通知消息列表
     */
    public List<NotifyMessagePO> listByUser(Long userId) {
        return notifyManager.list(Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getUserId, userId));
    }
}