package com.yirancrazy.minimall.pay.gateway;

import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PayRemoteGateway {


    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    Result<Long> nextId(String bizTag);


    /**
     * 执行 pay 方法。
     * @param orderId orderId 参数
     * @return 统一响应结果
     */
    Result<Void> pay(Long orderId);


    /**
     * 执行 payByOrderNo 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    Result<Void> payByOrderNo(String orderNo);


    /**
     * 执行 refundCallback 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    Result<Void> refundCallback(RefundCallbackDTO dto);


    /**
     * 执行 paySnapshot 方法。
     * @param orderNo orderNo 参数
     * @return 统一响应结果
     */
    Result<OrderPaySnapshotDTO> paySnapshot(String orderNo);
}
