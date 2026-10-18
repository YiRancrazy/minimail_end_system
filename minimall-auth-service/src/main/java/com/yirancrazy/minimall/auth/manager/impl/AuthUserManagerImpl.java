package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.mapper.AuthUserMapper;
import com.yirancrazy.minimall.common.annotation.Manager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthUserManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class AuthUserManagerImpl extends ServiceImpl<AuthUserMapper, AuthUserPO>
    implements AuthUserManager {
}
