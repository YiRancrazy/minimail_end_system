package com.yirancrazy.minimall.auth.gateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: Auth 服务远程调用网关。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
public interface AuthRemoteGateway {

    /**
     * 生成业务用户 ID。
     * @return 业务用户 ID
     */
    Long nextUserId();

    /**
     * 创建用户业务档案。
     * @param userId 业务用户 ID
     */
    void provisionUserProfile(Long userId);
}
