package com.yirancrazy.minimall.auth.gateway.impl;

import com.yirancrazy.minimall.api.dto.user.UserProvisionDto;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.gateway.AuthRemoteGateway;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证远程网关实现；通过 ID 服务生成业务用户 ID，再通过用户服务创建对应档案。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Component
@RequiredArgsConstructor
public class AuthRemoteGatewayImpl implements AuthRemoteGateway {

    private final IdFeignClient idFeignClient;
    private final UserFeignClient userFeignClient;

    /**
     * 调用 ID 服务获取全新的 USER 类型 ID，并校验统一返回结果和 ID 非空。
     *
     * @return 业务用户 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 远程结果失败或缺少数据时抛出
     */
    @Override
    public Long nextUserId() {
        Result<Long> result = idFeignClient.nextId("USER");
        if (result == null
                || !CommonCode.SUCCESS.equals(result.getCode())
                || result.getData() == null) {
            throw new BizException(AuthCodeEnum.USER_ID_GENERATE_FAIL);
        }
        return result.getData();
    }

    /**
     * 调用用户服务创建档案；该调用发生在认证账号落库之前，供注册事务边界控制。
     *
     * @param userId 业务用户 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户服务返回失败时抛出
     */
    @Override
    public void provisionUserProfile(Long userId) {
        Result<Void> result = userFeignClient.provision(new UserProvisionDto(userId));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            throw new BizException(AuthCodeEnum.USER_PROFILE_CREATE_FAIL);
        }
    }
}
