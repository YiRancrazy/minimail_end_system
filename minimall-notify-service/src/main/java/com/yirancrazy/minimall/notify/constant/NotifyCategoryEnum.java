package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义用户通知偏好的业务类别；偏好服务按该枚举补全缺失配置，并为站点、短信和邮件渠道提供默认开关.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum NotifyCategoryEnum implements BaseEnum {
  ORDER("ORDER", "订单通知"),
  PAYMENT("PAYMENT", "支付通知"),
  REFUND("REFUND", "退款通知"),
  STOCK("STOCK", "库存通知"),
  COMMENT("COMMENT", "评价回复通知"),
  ACTIVITY("ACTIVITY", "活动通知"),
  SYSTEM("SYSTEM", "系统通知");

  private final String code;
  private final String message;

  /**
   * 返回用于持久化和查询的通知类别别名字符串.
   *
   * @return 通知类别别名
   */
  @Override
  public String getAlias() {
    return code;
  }
}
