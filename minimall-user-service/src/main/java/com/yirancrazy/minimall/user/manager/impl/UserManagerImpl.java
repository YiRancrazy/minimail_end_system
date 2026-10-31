package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserPo;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.mapper.UserMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户档案 Manager 实现，将 UserMapper 与 UserPo 接入 MyBatis-Plus ServiceImpl 通用查询和持久化能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class UserManagerImpl extends ServiceImpl<UserMapper, UserPo> implements UserManager {}
