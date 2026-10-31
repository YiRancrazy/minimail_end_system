package com.yirancrazy.minimall.order.constant;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 区分用户结算来自购物车还是立即购买。
 *   业务边界：该枚举只用于请求分流，不直接持久化；不同来源对 items 与 cartItemIds 的互斥要求不同。
 *   使用场景：OrderCheckoutServiceImpl 在读取服务端购物车快照前据此选择处理路径。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
public enum CheckoutSourceEnum {
  CART,
  BUY_NOW
}
