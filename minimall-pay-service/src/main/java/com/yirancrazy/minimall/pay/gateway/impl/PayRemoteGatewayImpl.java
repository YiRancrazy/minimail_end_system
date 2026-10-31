package com.yirancrazy.minimall.pay.gateway.impl;

import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.gateway.PayRemoteGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将支付服务对 ID 服务和订单服务的调用适配为 Feign 请求。
 * 该类只转发统一响应，不吞掉远程失败，也不承担支付状态事务。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class PayRemoteGatewayImpl implements PayRemoteGateway {

  private final IdFeignClient idFeignClient;
  private final OrderFeignClient orderFeignClient;

  /**
   * 通过 ID Feign 客户端生成全局唯一标识。
   *
   * @param bizTag ID 业务标签
   * @return ID 服务原始统一响应
   */
  @Override
  public Result<Long> nextId(String bizTag) {
    return idFeignClient.nextId(bizTag);
  }

  /**
   * 通过订单 Feign 客户端按数字订单 ID 推进支付。
   *
   * @param orderId 数字订单 ID
   * @return 订单服务原始统一响应
   */
  @Override
  public Result<Void> pay(Long orderId) {
    return orderFeignClient.pay(orderId);
  }

  /**
   * 通过订单 Feign 客户端按业务订单号推进支付。
   *
   * @param orderNo 业务订单号
   * @return 订单服务原始统一响应
   */
  @Override
  public Result<Void> payByOrderNo(String orderNo) {
    return orderFeignClient.payByOrderNo(orderNo);
  }

  /**
   * 通过订单 Feign 客户端提交退款结果。
   *
   * @param dto 退款回调数据
   * @return 订单服务原始统一响应
   */
  @Override
  public Result<Void> refundCallback(RefundCallbackDto dto) {
    return orderFeignClient.refundCallback(dto);
  }

  /**
   * 通过订单 Feign 客户端读取支付校验快照。
   *
   * @param orderNo 业务订单号
   * @return 订单快照或失败码的统一响应
   */
  @Override
  public Result<OrderPaySnapshotDto> paySnapshot(String orderNo) {
    return orderFeignClient.paySnapshot(orderNo);
  }
}
