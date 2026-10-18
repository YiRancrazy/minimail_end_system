package com.yirancrazy.minimall.pay.gateway.impl;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.gateway.PayRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class PayRemoteGatewayImpl implements PayRemoteGateway {

    private final IdFeignClient idFeignClient;
    private final OrderFeignClient orderFeignClient;

    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Long> nextId(String bizTag) {
        return idFeignClient.nextId(bizTag);
    }

    /**
     * 执行 pay 方法。
     * @param orderId orderId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> pay(Long orderId) {
        return orderFeignClient.pay(orderId);
    }

    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> payByOrderNo(String orderNo) {
        return orderFeignClient.payByOrderNo(orderNo);
    }

    /**
     * 执行 refundCallback 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> refundCallback(RefundCallbackDTO dto) {
        return orderFeignClient.refundCallback(dto);
    }

    /**
     * 执行 paySnapshot 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    @Override
    public Result<OrderPaySnapshotDTO> paySnapshot(String orderNo) {
        return orderFeignClient.paySnapshot(orderNo);
    }
}
