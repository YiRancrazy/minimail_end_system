package com.yirancrazy.minimall.notify.gateway.impl;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class CommentRemoteGatewayImpl implements CommentRemoteGateway {

    private final OrderFeignClient orderFeignClient;

    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<OrderCommentEligibilityDTO> commentEligibility(String orderNo, Long userId, Long spuId) {
        return orderFeignClient.commentEligibility(orderNo, userId, spuId);
    }
}
