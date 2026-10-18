package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDTO;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDTO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface AuthService {

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
     * 执行 refreshToken 方法。
     * @param refreshToken refreshToken 参数
     * @return 处理结果
     */
    TokenVO refreshToken(String refreshToken);


    /**
     * 执行 signOut 方法。
     * @param userId userId 参数
     * @param jti jti 参数
     */
    void signOut(Long userId, String jti);


    /**
     * 执行 me 方法。
     * @param token token 参数
     * @return 处理结果
     */
    UserInfoVO me(String token);


    /**
     * 执行 changePassword 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     */
    void changePassword(Long userId, ChangePasswordDTO dto);


    /**
     * 发送业务消息。
     * @param dto dto 参数
     */
    void sendResetCode(SendResetCodeDTO dto);


    /**
     * 执行 resetPassword 方法。
     * @param dto dto 参数
     */
    void resetPassword(ResetPasswordDTO dto);
}
