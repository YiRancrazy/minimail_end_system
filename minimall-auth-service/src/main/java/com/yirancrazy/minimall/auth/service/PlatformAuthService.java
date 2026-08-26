package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminRoleDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.vo.AdminVO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证服务，提供平台管理员登录、登出与 CRUD 能力。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
public interface PlatformAuthService {

    /**
     * 平台管理员登录，仅允许 accountType=PLATFORM 的账号通过。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    TokenVO login(LoginDTO dto);

    /**
     * 平台管理员登出，失效当前账号所有刷新令牌并拉黑当前 jti。
     * @param adminAccountId 管理员账号ID
     * @param jti 令牌唯一标识
     */
    void signOut(Long adminAccountId, String jti);

    /**
     * 创建平台管理员，账号不可重复，密码 BCrypt 加密存储。
     * @param dto 创建入参
     * @return 新管理员ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号重复时
     */
    Long adminCreate(AdminCreateDTO dto);

    /**
     * 游标分页查询平台管理员，固定 accountType=PLATFORM，按 ID 降序返回。
     * @param dto 游标分页入参
     * @return 管理员游标分页结果
     */
    CursorPageVO<AdminVO> adminPage(AdminPageDTO dto);

    /**
     * 更新平台管理员昵称。
     * @param id 管理员ID
     * @param dto 更新入参
     * @return 更新是否成功
     * @throws com.yirancrazy.minimall.common.exception.BizException 管理员不存在时
     */
    boolean adminUpdate(Long id, AdminUpdateDTO dto);

    /**
     * 分配平台管理员角色，按角色编码解析角色并落库。
     * @param id 管理员ID
     * @param dto 角色分配入参
     * @throws com.yirancrazy.minimall.common.exception.BizException 管理员或角色不存在时
     */
    void adminAssignRole(Long id, AdminRoleDTO dto);

    /**
     * 平台管理员修改密码，校验旧密码后设置新密码并失效刷新令牌。
     * @param adminAccountId 管理员账号ID
     * @param dto 修改密码入参
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在或旧密码错误时
     */
    void changePassword(Long adminAccountId, ChangePasswordDTO dto);

    /**
     * 逻辑删除平台管理员，不允许删除自己。
     * @param operatorId 操作人ID
     * @param targetId 目标管理员ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 删除自己或管理员不存在时
     */
    void adminDelete(Long operatorId, Long targetId);

    /**
     * 获取当前平台管理员信息。
     * @param token 访问令牌
     * @return 用户信息VO
     * @throws com.yirancrazy.minimall.common.exception.BizException 令牌无效时
     */
    UserInfoVO me(String token);
}
