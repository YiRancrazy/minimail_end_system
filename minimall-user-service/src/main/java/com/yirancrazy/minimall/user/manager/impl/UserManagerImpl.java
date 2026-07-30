package com.yirancrazy.minimall.user.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.mapper.UserMapper;

/**
* 用户数据访问管理实现，继承 MyBatis-Plus 通用能力执行用户持久化操作。
 */
@Service
@Manager
public class UserManagerImpl extends ServiceImpl<UserMapper, UserPO>
    implements UserManager {
}