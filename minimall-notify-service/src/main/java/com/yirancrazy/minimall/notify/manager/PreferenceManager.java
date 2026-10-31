package com.yirancrazy.minimall.notify.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.entity.PreferencePo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 t_notify_preference 用户通知偏好的基础 MyBatis-Plus CRUD，不承载默认值和渠道校验规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PreferenceManager extends IService<PreferencePo> {}
