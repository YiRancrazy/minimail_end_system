package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户控制器，提供User RESTful API
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@RestController
@RequestMapping("/api/v1/user/users")
public class UserControllerV1 {

    private final UserService userService;

    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表，支持按用户名/昵称模糊搜索。仅平台管理员可访问。
     * @param dto 分页查询入参
     * @param role 网关注入的角色码（X-User-Role 头）
     * @return 用户分页结果
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @GetMapping
    public Result<CursorPageVO<UserVO>> page(@Valid UserPageDTO dto, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.page(dto).map(UserVO::from));
    }

    /**
     * 根据用户 ID 查询用户信息并返回用户持久化数据。仅平台管理员可访问。
     *
     * @param id 用户唯一标识
     * @param role 网关注入的角色码（X-User-Role 头）
     * @return 查询到的用户信息
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @GetMapping("/{id}")
    public Result<UserVO> get(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(UserVO.from(userService.getById(id)));
    }

    /**
     * 创建用户。仅平台管理员可访问。
     * @param dto 用户创建DTO
     * @param role 网关注入的角色码（X-User-Role 头）
     * @return 用户ID
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.create(dto));
    }

    /**
     * 按指定用户 ID 更新用户信息并返回更新是否成功。仅平台管理员可访问。
     *
     * @param id 用户唯一标识
     * @param dto 待更新的用户信息
     * @param role 网关注入的角色码（X-User-Role 头）
     * @return 更新是否成功
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateDTO dto,
                                  @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.update(id, dto));
    }

    /**
     * 删除用户。仅平台管理员可访问。
     * @param id 用户ID
     * @param role 网关注入的角色码（X-User-Role 头）
     * @return 删除是否成功
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.delete(id));
    }

    /**
     * 用户管理端点仅对平台管理员开放；网关已按 JWT role claim 注入 X-User-Role 头，客户端伪造头会被网关移除。
     * @param role 网关注入的角色码
     * @throws BizException 非 PLATFORM 角色访问时抛出 FORBIDDEN(20003)
     */
    private void requirePlatform(String role) {
        if (!RoleEnum.PLATFORM.getCode().equals(role)) {
            throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
        }
    }

    /**
     * 查询当前用户个人资料。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 用户信息
     */
    @GetMapping("/profile")
    public Result<UserVO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(UserVO.from(userService.getProfile(userId)));
    }

    /**
     * 更新个人资料，仅修改昵称、头像、性别。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param dto 个人资料更新入参
     * @return 更新是否成功
     */
    @PatchMapping("/profile")
    public Result<Boolean> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody UserProfileDTO dto) {
        return Result.success(userService.updateProfile(userId, dto));
    }
}