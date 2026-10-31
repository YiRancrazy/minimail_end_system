package com.yirancrazy.minimall.auth.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证账号数据访问边界，基于 AuthUserPo 提供 t_auth_user 的标准增删改查能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface AuthUserManager extends IService<AuthUserPo> {}
