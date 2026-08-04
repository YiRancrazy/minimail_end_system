package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.mapper.AuthUserMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthUser数据访问层实现，封装AuthUser表 CRUD 操作
 * @Version: 2.0
 * @DateTime: 2026/08/04
 */
@Manager
public class AuthUserManagerImpl extends ServiceImpl<AuthUserMapper, AuthUserPO>
    implements AuthUserManager {
}
