package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.PreferencePO;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.mapper.PreferenceMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好数据访问层实现，封装 t_notify_preference 表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Manager
public class PreferenceManagerImpl extends ServiceImpl<PreferenceMapper, PreferencePO>
    implements PreferenceManager {
}
