package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.PreferencePo;
import com.yirancrazy.minimall.notify.manager.PreferenceManager;
import com.yirancrazy.minimall.notify.mapper.PreferenceMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 PreferenceMapper 提供用户通知偏好的 MyBatis-Plus CRUD，默认开关和渠道校验由 PreferenceServiceImpl 负责.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class PreferenceManagerImpl extends ServiceImpl<PreferenceMapper, PreferencePo>
        implements PreferenceManager {}
