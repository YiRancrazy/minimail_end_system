package com.yirancrazy.minimall.auth.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.mapper.UserAuthMapper;
import com.yirancrazy.minimall.common.annotation.Manager;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证数据访问管理实现，基于 MyBatis-Plus 操作 UserAuthPO。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Service
@Manager
public class UserAuthManagerImpl extends ServiceImpl<UserAuthMapper, UserAuthPO>
    implements UserAuthManager {
}