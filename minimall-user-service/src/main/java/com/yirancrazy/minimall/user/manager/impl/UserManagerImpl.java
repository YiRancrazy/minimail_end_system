package com.yirancrazy.minimall.user.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.mapper.UserMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserManagerImpl description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserManagerImpl extends ServiceImpl<UserMapper, UserPO>
    implements UserManager {
}