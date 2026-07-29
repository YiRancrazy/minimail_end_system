package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户领域服务接口，定义用户查询、创建、更新与删除的业务契约。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface UserService {
    UserPO getById(Long id);

    Long create(UserCreateDTO dto);

    boolean update(Long id, UserUpdateDTO dto);

    boolean delete(Long id);
}