package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
 * 用户领域服务接口，定义用户查询、创建、更新、删除与个人资料管理业务契约。
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务接口，提供用户 CRUD、游标分页查询与个人资料管理业务契约。
 * @Version: 1.3
 * @DateTime: 2026/08/04
 */
public interface UserService {
    /**
     * 根据 ID 查询用户。
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
     * 游标分页查询用户，keyword 非空时按用户名/昵称模糊匹配。
     * @param dto 游标分页查询入参
     * @return 用户游标分页结果
     */
    CursorPageVO<UserPO> page(UserPageDTO dto);

    /**
     * 查询当前用户个人资料。
     * @param userId 用户ID
     * @return 用户PO
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时
     */
    UserPO getProfile(Long userId);

    /**
     * 更新个人资料，仅修改 nickname、avatar、gender 字段，null 字段不覆盖。
     * @param userId 用户ID
     * @param dto 个人资料更新入参
     * @return 更新是否成功
     */
    boolean updateProfile(Long userId, UserProfileDTO dto);
}
