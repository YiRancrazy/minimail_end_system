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
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.vo.AdminVO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证与管理控制器，提供平台管理员登录、登出与 CRUD 接口。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthControllerV1 {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthControllerV1(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    /**
     * 平台管理员登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(platformAuthService.login(dto));
    }

    /**
     * 平台管理员登出。
     * @param adminAccountId 管理员账号ID（来自网关 X-User-Id）
     * @param jti 令牌唯一标识（来自网关 X-User-Jti）
     * @return 无返回值
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long adminAccountId,
                               @RequestHeader("X-User-Jti") String jti) {
        platformAuthService.signOut(adminAccountId, jti);
        return Result.success();
    }

    /**
     * 获取当前平台管理员信息。
     * @param authorization 授权头
     * @return 用户信息VO
     */
    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ")
            ? authorization.substring(7) : authorization;
        return Result.success(platformAuthService.me(token));
    }

    /**
     * 创建平台管理员。
     * @param dto 创建入参
     * @return 新管理员ID
     */
    @PostMapping("/admins")
    public Result<Long> adminCreate(@Valid @RequestBody AdminCreateDTO dto) {
        return Result.success(platformAuthService.adminCreate(dto));
    }

    /**
     * 游标分页查询平台管理员。
     * @param dto 游标分页入参
     * @return 管理员游标分页结果
     */
    @GetMapping("/admins")
    public Result<CursorPageVO<AdminVO>> adminPage(@Valid AdminPageDTO dto) {
        return Result.success(platformAuthService.adminPage(dto));
    }

    /**
     * 更新平台管理员昵称。
     * @param id 管理员ID
     * @param dto 更新入参
     * @return 更新是否成功
     */
    @PutMapping("/admins/{id}")
    public Result<Boolean> adminUpdate(@PathVariable("id") Long id,
                                       @Valid @RequestBody AdminUpdateDTO dto) {
        return Result.success(platformAuthService.adminUpdate(id, dto));
    }

    /**
     * 逻辑删除平台管理员，不允许删除自己。
     * @param operatorId 操作人ID（来自网关 X-User-Id）
     * @param id 目标管理员ID
     * @return 无业务数据的成功响应
     */
    @DeleteMapping("/admins/{id}")
    public Result<Void> adminDelete(@RequestHeader("X-User-Id") Long operatorId,
                                    @PathVariable("id") Long id) {
        platformAuthService.adminDelete(operatorId, id);
        return Result.success(null);
    }
}
