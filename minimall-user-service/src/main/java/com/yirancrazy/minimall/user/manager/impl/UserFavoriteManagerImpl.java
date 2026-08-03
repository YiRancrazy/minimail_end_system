package com.yirancrazy.minimall.user.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.user.entity.UserFavoritePO;
import com.yirancrazy.minimall.user.manager.UserFavoriteManager;
import com.yirancrazy.minimall.user.mapper.UserFavoriteMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFavorite数据访问层实现，封装用户收藏表CRUD操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Manager
public class UserFavoriteManagerImpl extends ServiceImpl<UserFavoriteMapper, UserFavoritePO>
    implements UserFavoriteManager {
}
