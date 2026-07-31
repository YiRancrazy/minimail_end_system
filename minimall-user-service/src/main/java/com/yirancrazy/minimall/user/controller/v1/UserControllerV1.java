package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserControllerV1 description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserControllerV1 {

    private final UserService userService;

    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }

    /**
     * 根据用户 ID 查询用户信息并返回用户持久化数据。
     *
     * @param id 用户唯一标识
     * @return 查询到的用户信息
     */
    @GetMapping("/{id}")
    public Result<UserPO> get(@PathVariable("id") Long id) {
        return Result.success(userService.getById(id));
    }

    /**
     * 创建用户。
     * @param dto 用户创建DTO
     * @return 用户ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userService.create(dto));
    }

    /**
     * 按指定用户 ID 更新用户信息并返回更新是否成功。
     *
     * @param id 用户唯一标识
     * @param dto 待更新的用户信息
     * @return 更新是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userService.update(id, dto));
    }

    /**
     * 删除用户。
     * @param id 用户ID
     * @return 删除是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.success(userService.delete(id));
    }
}