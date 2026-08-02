package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证服务，提供平台管理员登录、登出能力。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface PlatformAuthService {

    /**
     * 平台管理员登录，仅允许 role=PLATFORM 的账号通过。
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
}
