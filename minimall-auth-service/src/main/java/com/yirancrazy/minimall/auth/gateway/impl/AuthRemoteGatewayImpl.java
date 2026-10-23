package com.yirancrazy.minimall.auth.gateway.impl;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.user.UserProvisionDTO;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.gateway.AuthRemoteGateway;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: Auth 服务远程调用网关实现。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Component
@RequiredArgsConstructor
public class AuthRemoteGatewayImpl implements AuthRemoteGateway {

    private final IdFeignClient idFeignClient;
    private final UserFeignClient userFeignClient;

    /**
     * 生成业务用户 ID。
     * @return 业务用户 ID
     */
    @Override
    public Long nextUserId() {
        Result<Long> result = idFeignClient.nextId("USER");
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            throw new BizException(AuthCodeEnum.USER_ID_GENERATE_FAIL);
        }
        return result.getData();
    }

    /**
     * 创建用户业务档案。
     * @param userId 业务用户 ID
     */
    @Override
    public void provisionUserProfile(Long userId) {
        Result<Void> result = userFeignClient.provision(new UserProvisionDTO(userId));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            throw new BizException(AuthCodeEnum.USER_PROFILE_CREATE_FAIL);
        }
    }
}
