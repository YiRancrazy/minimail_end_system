package com.yirancrazy.minimall.user.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.UserService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户领域服务实现，实现User相关业务逻辑
 * @Version: 1.1
 * @DateTime: 2026/07/31
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserManager userManager;

    public UserServiceImpl(UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * 根据用户 ID 查询用户信息，不存在时抛出用户不存在业务异常。
     *
     * @param id 用户唯一标识
     * @return 查询到的用户持久化实体
     */
    @Override
    public UserPO getById(Long id) {
        UserPO u = userManager.getById(id);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }

    /**
     * 保存新用户信息并返回持久化后生成的用户 ID。
     *
     * @param dto 待创建的用户信息
     * @return 新创建用户的唯一标识
     */
    @Override
    public Long create(UserCreateDTO dto) {
        UserPO user = new UserPO();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userManager.save(user);
        return user.getId();
    }

    /**
     * 将指定用户 ID 写入待更新实体并执行用户信息更新。
     *
     * @param id 用户唯一标识
     * @param dto 待更新的用户信息
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, UserUpdateDTO dto) {
        UserPO user = new UserPO();
        user.setId(id);
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        return userManager.updateById(user);
    }

    /**
     * 根据用户 ID 删除用户记录并返回删除是否成功。
     *
     * @param id 用户唯一标识
     * @return 删除是否成功
     */
    @Override
    public boolean delete(Long id) {
        return userManager.removeById(id);
    }

    /**
     * 分页查询用户，keyword 非空时按用户名或昵称模糊匹配。
     *
     * @param dto 分页查询入参
     * @return 用户分页结果
     */
    @Override
    public IPage<UserPO> page(UserPageDTO dto) {
        Page<UserPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return userManager.page(page, Wrappers.lambdaQuery(UserPO.class)
            .and(dto.getKeyword() != null && !dto.getKeyword().isBlank(),
                w -> w.like(UserPO::getUsername, dto.getKeyword())
                    .or().like(UserPO::getNickname, dto.getKeyword())));
    }
}