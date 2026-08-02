package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端认证服务，提供商家登录、登出、修改密码能力。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface MerchantAuthService {

    /**
     * 商家登录，仅允许 role=MERCHANT 的账号通过。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    TokenVO login(LoginDTO dto);

    /**
     * 商家登出，失效当前账号所有刷新令牌并拉黑当前 jti。
     * @param merchantAccountId 商家账号ID
     * @param jti 令牌唯一标识
     */
    void signOut(Long merchantAccountId, String jti);

    /**
     * 商家修改密码，校验旧密码后设置新密码并失效已有刷新令牌。
     * @param merchantAccountId 商家账号ID
     * @param dto 修改密码DTO
     */
    void changePassword(Long merchantAccountId, ChangePasswordDTO dto);
}
