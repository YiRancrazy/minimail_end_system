package com.yirancrazy.minimall.user.service.impl;

import com.yirancrazy.minimall.user.constant.UserCodeEnum;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserManager userManager;

    public UserServiceImpl(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public UserPO getById(Long id) {
        UserPO u = userManager.getById(id);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }

    @Override
    public Long create(UserPO user) {
        userManager.save(user);
        return user.getId();
    }

    @Override
    public boolean update(Long id, UserPO user) {
        user.setId(id);
        return userManager.updateById(user);
    }

    @Override
    public boolean delete(Long id) {
        return userManager.removeById(id);
    }
}