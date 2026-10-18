package com.yirancrazy.minimall.auth.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminRoleDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.vo.AdminVO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformAuthControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthControllerV1 {

    private final PlatformAuthService platformAuthService;

    /**
     * 构造 PlatformAuthControllerV1 实例。
     * @param platformAuthService platformAuthService 参数
     */
    public PlatformAuthControllerV1(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }


    /**
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(platformAuthService.login(dto));
    }


    /**
     * 执行 logout 方法。
     * @param adminAccountId adminAccountId 参数
     * @param jti jti 参数
     * @return 统一响应结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long adminAccountId,
                               @RequestHeader("X-User-Jti") String jti) {
        platformAuthService.signOut(adminAccountId, jti);
        return Result.success();
    }


    /**
     * 执行 me 方法。
     * @param authorization authorization 参数
     * @return 统一响应结果
     */
    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ")
            ? authorization.substring(7) : authorization;
        return Result.success(platformAuthService.me(token));
    }


    /**
     * 执行 adminCreate 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/admins")
    public Result<Long> adminCreate(@Valid @RequestBody AdminCreateDTO dto) {
        return Result.success(platformAuthService.adminCreate(dto));
    }


    /**
     * 执行 adminPage 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/admins")
    public Result<CursorPageVO<AdminVO>> adminPage(@Valid AdminPageDTO dto) {
        return Result.success(platformAuthService.adminPage(dto));
    }


    /**
     * 执行 adminUpdate 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/admins/{id}")
    public Result<Boolean> adminUpdate(@PathVariable("id") Long id,
                                       @Valid @RequestBody AdminUpdateDTO dto) {
        return Result.success(platformAuthService.adminUpdate(id, dto));
    }


    /**
     * 执行 adminAssignRole 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/admins/{id}/role")
    public Result<Void> adminAssignRole(@PathVariable("id") Long id,
                                        @Valid @RequestBody AdminRoleDTO dto) {
        platformAuthService.adminAssignRole(id, dto);
        return Result.success(null);
    }


    /**
     * 执行 changePassword 方法。
     * @param adminAccountId adminAccountId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long adminAccountId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        platformAuthService.changePassword(adminAccountId, dto);
        return Result.success(null);
    }


    /**
     * 执行 adminDelete 方法。
     * @param operatorId operatorId 参数
     * @param id id 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/admins/{id}")
    public Result<Void> adminDelete(@RequestHeader("X-User-Id") Long operatorId,
                                    @PathVariable("id") Long id) {
        platformAuthService.adminDelete(operatorId, id);
        return Result.success(null);
    }
}
