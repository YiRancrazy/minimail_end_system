package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.mapper.AuthRoleMapper;
import com.yirancrazy.minimall.common.annotation.Manager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthRole数据访问层实现，封装AuthRole表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Manager
public class AuthRoleManagerImpl extends ServiceImpl<AuthRoleMapper, AuthRolePO>
    implements AuthRoleManager {
}
