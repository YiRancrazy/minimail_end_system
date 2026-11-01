package com.yirancrazy.minimall.pay.gateway;

import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 抽象支付服务依赖的远程 ID 生成和订单状态能力。
 * 隔离 Feign 契约，使支付业务可通过替身测试远程失败、订单推进和快照读取。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PayRemoteGateway {

    /**
     * 请求 ID 服务为指定业务标签生成全局唯一标识。
     *
     * @param bizTag ID 业务标签，例如 PAY 或 REFUND
     * @return 包含全局 ID 或失败码的统一响应
     */
    Result<Long> nextId(String bizTag);

    /**
     * 按数字订单 ID 通知订单服务推进支付状态。
     *
     * @param orderId 数字订单 ID
     * @return 订单服务是否成功接受按订单 ID 的支付推进
     */
    Result<Void> pay(Long orderId);

    /**
     * 按非数字订单号通知订单服务推进支付状态。
     *
     * @param orderNo 业务订单号
     * @return 订单服务是否成功接受按业务订单号的支付推进
     */
    Result<Void> payByOrderNo(String orderNo);

    /**
     * 将退款结果通知订单服务。
     *
     * @param dto 订单 ID、退款金额、累计退款和是否全额退款信息
     * @return 订单服务是否成功接受退款结果
     */
    Result<Void> refundCallback(RefundCallbackDto dto);

    /**
     * 读取订单服务的支付快照，作为金额、归属和订单状态校验依据。
     *
     * @param orderNo 业务订单号
     * @return 包含应付金额、用户、商家和订单状态的统一响应
     */
    Result<OrderPaySnapshotDto> paySnapshot(String orderNo);
}
