package com.yirancrazy.minimall.stock.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义库存流水对应的预占、释放、调整、调拨和纠正业务类型。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum StockJournalTypeEnum implements BaseEnum {
  RESERVE(1, "RESERVE", "预占"),
  RELEASE(2, "RELEASE", "释放"),
  ADJUST(3, "ADJUST", "调整"),
  TRANSFER_OUT(4, "TRANSFER_OUT", "调出"),
  TRANSFER_IN(5, "TRANSFER_IN", "调入"),
  CORRECT(6, "CORRECT", "纠正");

  private final int code;
  private final String alias;
  private final String message;

  /**
   * 将流水类型的整数编码转换为统一枚举接口所需的字符串。
   *
   * @return 十进制字符串形式的流水类型编码
   */
  @Override
  public String getCode() {
    return String.valueOf(code);
  }

  /**
   * 返回写入库存流水和参与业务判断时使用的整数编码。
   *
   * @return 流水类型的整数编码
   */
  public int intCode() {
    return code;
  }
}
