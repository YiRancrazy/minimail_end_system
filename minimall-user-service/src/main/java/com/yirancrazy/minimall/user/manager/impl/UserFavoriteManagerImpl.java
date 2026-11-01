package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserFavoritePo;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.mapper.UserFavoriteMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏 Manager 实现，将 UserFavoriteMapper 与 UserFavoritePo 接入 MyBatis-Plus ServiceImpl 通用查询和持久化能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class UserFavoriteManagerImpl extends ServiceImpl<UserFavoriteMapper, UserFavoritePo>
        implements UserFavoriteManager {}
