package com.yirancrazy.minimall.stock.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义盘点任务从待盘点到完成或取消的生命周期状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum StockCountTaskStatusEnum implements BaseEnum {
  PENDING(1, "PENDING", "待盘点"),
  COMPLETED(2, "COMPLETED", "已完成"),
  CANCELLED(3, "CANCELLED", "已取消");

  private final int code;
  private final String alias;
  private final String message;

  /**
   * 将持久化使用的整数状态码转换为统一枚举接口所需的字符串。
   *
   * @return 十进制字符串形式的盘点任务状态码
   */
  @Override
  public String getCode() {
    return String.valueOf(code);
  }

  /**
   * 返回用于数据库持久化和状态比较的原始整数编码。
   *
   * @return 盘点任务状态的整数编码
   */
  public int intCode() {
    return code;
  }
}
