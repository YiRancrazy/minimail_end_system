package com.yirancrazy.minimall.notify.gateway.impl;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.gateway.CommentRemoteGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过 OrderFeignClient 调用订单服务的内部评价资格接口，并原样返回远程 Result 供评价服务判定.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class CommentRemoteGatewayImpl implements CommentRemoteGateway {

  private final OrderFeignClient orderFeignClient;

  /**
   * 调用订单服务查询指定订单商品的评价资格.
   *
   * @param orderNo 待评价订单编号
   * @param userId 发起评价的用户编号
   * @param spuId 待评价商品 SPU 编号
   * @return 订单服务的评价资格响应；网络或服务端错误由 Feign 调用链抛出
   */
  @Override
  public Result<OrderCommentEligibilityDto> commentEligibility(
      String orderNo, Long userId, Long spuId) {
    return orderFeignClient.commentEligibility(orderNo, userId, spuId);
  }
}
