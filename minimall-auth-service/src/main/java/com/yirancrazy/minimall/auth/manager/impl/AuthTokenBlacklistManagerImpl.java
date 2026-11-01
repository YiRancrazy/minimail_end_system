package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.mapper.AuthTokenBlacklistMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 撤销记录 Manager 实现；通过 AuthTokenBlacklistMapper 持久化 t_auth_token_blacklist。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class AuthTokenBlacklistManagerImpl
        extends ServiceImpl<AuthTokenBlacklistMapper, AuthTokenBlacklistPo>
        implements AuthTokenBlacklistManager {}
