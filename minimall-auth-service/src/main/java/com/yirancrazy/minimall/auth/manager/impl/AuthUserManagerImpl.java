package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.mapper.AuthUserMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证账号 Manager 实现；通过 AuthUserMapper 将 MyBatis-Plus 通用操作落到 t_auth_user。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class AuthUserManagerImpl extends ServiceImpl<AuthUserMapper, AuthUserPo>
        implements AuthUserManager {}
