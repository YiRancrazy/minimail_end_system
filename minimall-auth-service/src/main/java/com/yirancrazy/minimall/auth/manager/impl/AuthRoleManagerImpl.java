package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.mapper.AuthRoleMapper;
import com.yirancrazy.minimall.common.annotation.Manager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthRoleManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class AuthRoleManagerImpl extends ServiceImpl<AuthRoleMapper, AuthRolePO>
    implements AuthRoleManager {
}
