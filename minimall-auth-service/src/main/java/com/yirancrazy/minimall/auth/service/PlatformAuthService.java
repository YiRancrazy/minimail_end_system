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
 * @Description: PlatformAuthService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformAuthService {


    /**
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    TokenVO login(LoginDTO dto);


    /**
     * 执行 signOut 方法。
     * @param adminAccountId adminAccountId 参数
     * @param jti jti 参数
     */
    void signOut(Long adminAccountId, String jti);


    /**
     * 执行 adminCreate 方法。
     * @param dto dto 参数
     * @return 业务数值
     */
    Long adminCreate(AdminCreateDTO dto);


    /**
     * 执行 adminPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<AdminVO> adminPage(AdminPageDTO dto);


    /**
     * 执行 adminUpdate 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean adminUpdate(Long id, AdminUpdateDTO dto);


    /**
     * 执行 adminAssignRole 方法。
     * @param id id 参数
     * @param dto dto 参数
     */
    void adminAssignRole(Long id, AdminRoleDTO dto);


    /**
     * 执行 changePassword 方法。
     * @param adminAccountId adminAccountId 参数
     * @param dto dto 参数
     */
    void changePassword(Long adminAccountId, ChangePasswordDTO dto);


    /**
     * 执行 adminDelete 方法。
     * @param operatorId operatorId 参数
     * @param targetId targetId 参数
     */
    void adminDelete(Long operatorId, Long targetId);


    /**
     * 执行 me 方法。
     * @param token token 参数
     * @return 处理结果
     */
    UserInfoVO me(String token);
}
