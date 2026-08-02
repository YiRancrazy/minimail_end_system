package com.yirancrazy.minimall.notify.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.manager.NotifyManager;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知领域服务实现，实现Notify相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Service
public class NotifyServiceImpl implements NotifyService {

    private final NotifyManager notifyManager;

    public NotifyServiceImpl(NotifyManager notifyManager) {
        this.notifyManager = notifyManager;
    }

    @Override
    public boolean push(Long userId, String title, String content) {
        NotifyMessagePO m = new NotifyMessagePO();
        m.setUserId(userId);
        m.setTitle(title);
        m.setContent(content);
        m.setReadFlag(0);
        return notifyManager.save(m);
    }

    @Override
    public List<NotifyMessagePO> listByUser(NotifyListDTO dto) {
        return notifyManager.list(Wrappers.lambdaQuery(NotifyMessagePO.class)
            .eq(NotifyMessagePO::getUserId, dto.getUserId()));
    }
}
