package com.yirancrazy.minimall.notify.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 t_notify_message 站内通知的基础 MyBatis-Plus CRUD，不承载广播、已读和 SSE 业务规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface NotifyManager extends IService<NotifyMessagePo> {}
