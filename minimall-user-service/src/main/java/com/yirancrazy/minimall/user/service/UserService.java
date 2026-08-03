package com.yirancrazy.minimall.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
 * 用户领域服务接口，定义用户查询、创建、更新与删除的业务契约。
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务接口，提供用户CRUD与分页查询业务契约。
 * @Version: 1.1
 * @DateTime: 2026/7/31
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

    /**
     * 分页查询用户，keyword 非空时按用户名/昵称模糊匹配。
     * @param dto 分页查询入参
     * @return 用户分页结果
     */
    IPage<UserPO> page(UserPageDTO dto);
}