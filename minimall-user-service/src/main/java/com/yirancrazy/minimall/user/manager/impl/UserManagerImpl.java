package com.yirancrazy.minimall.user.manager.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.mapper.UserMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: User数据访问层实现，封装User表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserManagerImpl extends ServiceImpl<UserMapper, UserPO>
    implements UserManager {
}