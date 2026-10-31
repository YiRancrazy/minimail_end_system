package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义商品审核通过、驳回的持久化决策码及展示文案。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum AuditDecisionEnum {
  APPROVE(1, "APPROVE", "通过"),
  REJECT(2, "REJECT", "驳回");

  private final int code;
  private final String alias;
  private final String message;
}
