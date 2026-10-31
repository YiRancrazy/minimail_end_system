package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 记录取消订单后的库存释放处理状态。
 *   业务边界：状态独立于订单主状态，用于区分无需释放、待补偿和已释放；实际库存调用由订单服务发起。
 *   使用场景：用户取消、商家关闭、平台关闭及补偿扫描共用该状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum StockReleaseStatusEnum implements BaseEnum {
  NOT_REQUIRED(0, "NOT_REQUIRED", "无需释放"),
  PENDING(1, "PENDING", "待释放"),
  RELEASED(2, "RELEASED", "已释放");

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
