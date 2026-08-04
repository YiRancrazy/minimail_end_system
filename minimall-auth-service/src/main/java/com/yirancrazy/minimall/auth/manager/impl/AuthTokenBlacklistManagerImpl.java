package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPO;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.mapper.AuthTokenBlacklistMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthTokenBlacklist数据访问层实现，封装Token黑名单表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Manager
public class AuthTokenBlacklistManagerImpl extends ServiceImpl<AuthTokenBlacklistMapper, AuthTokenBlacklistPO>
    implements AuthTokenBlacklistManager {
}
