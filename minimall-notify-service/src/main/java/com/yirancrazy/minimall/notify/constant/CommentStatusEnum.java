package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义商品评价在审核与删除流程中的持久化状态，服务层据此控制公开查询和商家回复的可见范围.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum CommentStatusEnum implements BaseEnum {
  NORMAL(1, "NORMAL", "正常"),
  HIDDEN(2, "HIDDEN", "已隐藏"),
  DELETED(3, "DELETED", "已删除");

  private final int code;
  private final String alias;
  private final String message;

  /**
   * 将评价状态编码转换为 BaseEnum 约定的字符串形式.
   *
   * @return 评价状态的十进制字符串编码
   */
  @Override
  public String getCode() {
    return String.valueOf(code);
  }

  /**
   * 返回可直接用于数据库状态字段比较的整数编码.
   *
   * @return 评价状态整数编码
   */
  public int intCode() {
    return code;
  }
}
