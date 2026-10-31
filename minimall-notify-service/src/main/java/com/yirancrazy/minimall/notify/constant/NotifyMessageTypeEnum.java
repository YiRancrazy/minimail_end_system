package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义站内通知的业务类型，用于订单、物流、退款、营销、公告、系统和违规消息的分类存储与展示.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum NotifyMessageTypeEnum implements BaseEnum {
  ORDER(1, "ORDER", "订单"),
  LOGISTICS(2, "LOGISTICS", "物流"),
  REFUND(3, "REFUND", "退款"),
  PROMOTION(4, "PROMOTION", "营销"),
  ANNOUNCEMENT(5, "ANNOUNCEMENT", "公告"),
  SYSTEM(6, "SYSTEM", "系统"),
  VIOLATION(7, "VIOLATION", "违规");

  private final int code;
  private final String alias;
  private final String message;

  /**
   * 将通知类型编码转换为 BaseEnum 约定的字符串形式.
   *
   * @return 通知类型的十进制字符串编码
   */
  @Override
  public String getCode() {
    return String.valueOf(code);
  }

  /**
   * 返回可直接用于数据库消息类型字段比较的整数编码.
   *
   * @return 通知类型整数编码
   */
  public int intCode() {
    return code;
  }
}
