package com.yirancrazy.minimall.notify.gateway;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CommentRemoteGateway {


    /**
     * 执行 commentEligibility 方法。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    Result<OrderCommentEligibilityDTO> commentEligibility(String orderNo, Long userId, Long spuId);
}
