package com.yirancrazy.minimall.order.dto;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载下单时的收货人及地址快照。
 *   业务边界：仅在订单创建时序列化保存，不直接查询用户地址服务，也不承担地址合法性校验。
 *   使用场景：单 SKU 创建和结算接口的请求体使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ReceiverDto {

  private String receiverName;

  private String receiverPhone;

  private String province;

  private String city;

  private String district;

  private String detailAddress;
}
