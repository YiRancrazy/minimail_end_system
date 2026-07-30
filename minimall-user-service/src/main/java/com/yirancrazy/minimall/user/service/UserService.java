package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
* 用户领域服务接口，定义用户查询、创建、更新与删除的业务契约。
 */
public interface UserService {
    /**
     * 根据ID查询用户。
     * @param id 用户ID
     * @return 用户PO
     */
    UserPO getById(Long id);

    /**
     * 创建用户。
     * @param dto 用户创建DTO
     * @return 用户ID
     */
    Long create(UserCreateDTO dto);

    /**
     * 更新用户。
     * @param id 用户ID
     * @param dto 用户更新DTO
     * @return 更新是否成功
     */
    boolean update(Long id, UserUpdateDTO dto);

    /**
     * 删除用户。
     * @param id 用户ID
     * @return 删除是否成功
     */
    boolean delete(Long id);
}