package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.mapper.AuthRoleMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证角色 Manager 实现；通过 AuthRoleMapper 将 MyBatis-Plus 通用操作落到 t_auth_role。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class AuthRoleManagerImpl extends ServiceImpl<AuthRoleMapper, AuthRolePo>
        implements AuthRoleManager {}
