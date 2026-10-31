package com.yirancrazy.minimall.notify.gateway;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 隔离评价服务对订单服务的远程依赖，用于查询当前用户是否具备指定订单商品的有效评价资格.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CommentRemoteGateway {

  /**
   * 查询用户对指定订单商品的可评价状态及订单所属商家信息.
   *
   * @param orderNo 待评价订单编号
   * @param userId 发起评价的用户编号
   * @param spuId 待评价商品 SPU 编号
   * @return 订单服务返回的评价资格结果；调用失败时由实现方返回失败结果或由调用方转换为业务异常
   */
  Result<OrderCommentEligibilityDto> commentEligibility(String orderNo, Long userId, Long spuId);
}
