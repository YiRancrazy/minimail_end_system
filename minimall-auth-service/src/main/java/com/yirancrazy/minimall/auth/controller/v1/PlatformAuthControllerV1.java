package com.yirancrazy.minimall.auth.controller.v1;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.AdminCreateDto;
import com.yirancrazy.minimall.auth.dto.AdminPageDto;
import com.yirancrazy.minimall.auth.dto.AdminRoleDto;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.vo.AdminVo;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员认证与管理 HTTP 边界；覆盖登录、退出、当前账号、管理员 CRUD 和密码修改。
 * 操作者身份来自网关请求头，业务规则和角色解析由 PlatformAuthService 承担。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthControllerV1 {

    private final PlatformAuthService platformAuthService;

    /**
     * 注入平台认证与管理服务。
     *
     * @param platformAuthService 平台认证和管理员管理业务入口
     */
    public PlatformAuthControllerV1(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    /**
     * 校验平台管理员账号类型、状态和密码后签发令牌。
     *
     * @param dto 平台管理员登录账号和密码
     * @return 成功响应，数据为签发令牌
     */
    @PostMapping("/login")
    public Result<TokenVo> login(@Valid @RequestBody LoginDto dto) {
        return Result.success(platformAuthService.login(dto));
    }

    /**
     * 退出当前平台管理员会话，并撤销当前 JWT。
     *
     * @param adminAccountId 网关注入的平台管理员账号 ID
     * @param jti 网关注入的当前 JWT 唯一标识
     * @return 成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader("X-Auth-User-Id") Long adminAccountId,
            @RequestHeader("X-User-Jti") String jti) {
        platformAuthService.signOut(adminAccountId, jti);
        return Result.success();
    }

    /**
     * 查询当前平台管理员信息；Authorization 可带或不带 Bearer 前缀。
     *
     * @param authorization 当前请求的 JWT 或 Bearer JWT
     * @return 成功响应，数据为 JWT 中的安全用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoVo> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        return Result.success(platformAuthService.me(token));
    }

    /**
     * 创建平台管理员账号并返回认证账号主键。
     *
     * @param dto 管理员账号、密码、可选联系方式和角色编码
     * @return 成功响应，数据为新建管理员 ID
     */
    @PostMapping("/admins")
    public Result<Long> adminCreate(@Valid @RequestBody AdminCreateDto dto) {
        return Result.success(platformAuthService.adminCreate(dto));
    }

    /**
     * 按游标倒序分页查询平台管理员。
     *
     * @param dto 游标和每页数量
     * @return 成功响应，数据为管理员游标分页结果
     */
    @GetMapping("/admins")
    public Result<CursorPageVo<AdminVo>> adminPage(@Valid AdminPageDto dto) {
        return Result.success(platformAuthService.adminPage(dto));
    }

    /**
     * 将指定平台管理员更新为新的角色编码。
     *
     * @param id 目标管理员认证账号 ID
     * @param dto 新角色编码
     * @return 成功响应
     */
    @PutMapping("/admins/{id}/role")
    public Result<Void> adminAssignRole(
            @PathVariable("id") Long id, @Valid @RequestBody AdminRoleDto dto) {
        platformAuthService.adminAssignRole(id, dto);
        return Result.success(null);
    }

    /**
     * 修改当前平台管理员密码，并使该账号已有刷新令牌失效。
     *
     * @param adminAccountId 网关注入的平台管理员账号 ID
     * @param dto 旧密码和新密码
     * @return 成功响应
     */
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader("X-Auth-User-Id") Long adminAccountId,
            @Valid @RequestBody ChangePasswordDto dto) {
        platformAuthService.changePassword(adminAccountId, dto);
        return Result.success(null);
    }

    /**
     * 删除指定平台管理员，禁止删除当前操作者自身。
     *
     * @param operatorId 当前操作者账号 ID
     * @param id 待删除管理员账号 ID
     * @return 成功响应
     */
    @DeleteMapping("/admins/{id}")
    public Result<Void> adminDelete(
            @RequestHeader("X-Auth-User-Id") Long operatorId, @PathVariable("id") Long id) {
        platformAuthService.adminDelete(operatorId, id);
        return Result.success(null);
    }
}
