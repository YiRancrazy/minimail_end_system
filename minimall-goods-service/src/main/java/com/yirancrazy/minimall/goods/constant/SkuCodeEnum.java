package com.yirancrazy.minimall.goods.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SKU 查询和库存调整失败时对外暴露的业务错误码。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum SkuCodeEnum implements BaseEnum {
  SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在"),
  STOCK_ADJUST_FAILED("13002", "STOCK_ADJUST_FAILED", "库存服务调整失败");

  private final String code;
  private final String alias;
  private final String message;
}
