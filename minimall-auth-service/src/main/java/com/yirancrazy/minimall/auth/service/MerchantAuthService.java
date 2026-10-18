package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantAuthService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface MerchantAuthService {


    /**
     * 执行 register 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    TokenVO register(RegisterDTO dto);


    /**
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    TokenVO login(LoginDTO dto);


    /**
     * 执行 signOut 方法。
     * @param merchantAccountId merchantAccountId 参数
     * @param jti jti 参数
     */
    void signOut(Long merchantAccountId, String jti);


    /**
     * 执行 changePassword 方法。
     * @param merchantAccountId merchantAccountId 参数
     * @param dto dto 参数
     */
    void changePassword(Long merchantAccountId, ChangePasswordDTO dto);


    /**
     * 执行 me 方法。
     * @param token token 参数
     * @return 处理结果
     */
    UserInfoVO me(String token);
}
