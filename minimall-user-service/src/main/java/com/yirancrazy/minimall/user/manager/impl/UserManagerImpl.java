package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
@Manager
public class UserManagerImpl extends ServiceImpl<UserMapper, UserPO>
    implements UserManager {
}