package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单从待支付到已退款的主状态及其中间退款状态。
 *   业务边界：整数 code 是数据库持久化值，alias 是外部协议值；状态合法性由 OrderStatusMachine 校验。
 *   使用场景：订单读写、筛选、状态日志和前端展示统一使用该枚举。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum implements BaseEnum {
  PENDING(1, "PENDING", "待支付"),
  PAID(2, "PAID", "已支付"),
  SHIPPED(3, "SHIPPED", "已发货"),
  COMPLETED(4, "COMPLETED", "已完成"),
  CANCELED(5, "CANCELED", "已取消"),
  REFUNDING(6, "REFUNDING", "退款中"),
  REFUNDED(7, "REFUNDED", "已退款");

  private final int code;
  private final String alias;
  private final String message;

  /**
   * 返回枚举的字符串编码。
   *
   * <p>用于 BaseEnum 统一协议，不改变持久化 code。
   *
   * @return 十进制字符串编码。
   */
  @Override
  public String getCode() {
    return String.valueOf(code);
  }

  /**
   * 返回枚举整数编码。
   *
   * <p>供数据库持久化和条件查询使用。
   *
   * @return 整数编码。
   */
  public int intCode() {
    return code;
  }
}
