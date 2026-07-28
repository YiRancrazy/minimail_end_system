package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.user.entity.UserPO;

public interface UserService {
    UserPO getById(Long id);

    Long create(UserPO user);

    boolean update(Long id, UserPO user);

    boolean delete(Long id);
}