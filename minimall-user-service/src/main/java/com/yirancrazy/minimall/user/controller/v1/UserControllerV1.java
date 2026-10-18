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
 * @Description: UserControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/users")
public class UserControllerV1 {

    private final UserService userService;

    /**
     * 构造 UserControllerV1 实例。
     * @param userService userService 参数
     */
    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<UserVO>> page(@Valid UserPageDTO dto, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.pageVO(dto));
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<UserVO> get(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.getVO(id));
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.create(dto));
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param dto dto 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateDTO dto,
                                  @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.update(id, dto));
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param role role 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
        requirePlatform(role);
        return Result.success(userService.delete(id));
    }


    /**
     * 处理require平台。
     * @param role 角色
     * @throws BizException 业务校验失败时抛出
     */
    private void requirePlatform(String role) {
        if (!RoleEnum.PLATFORM.getCode().equals(role)) {
            throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
        }
    }


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/profile")
    public Result<UserVO> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(userService.getProfileVO(userId));
    }


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PatchMapping("/profile")
    public Result<Boolean> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody UserProfileDTO dto) {
        return Result.success(userService.updateProfile(userId, dto));
    }
}
