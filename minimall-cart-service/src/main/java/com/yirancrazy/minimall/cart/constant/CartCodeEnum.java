package com.yirancrazy.minimall.cart.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义购物车域稳定的错误码及客户端提示信息。编码覆盖条目缺失、未勾选、结算快照
 *     不一致、移入收藏失败、结算清理失败和内部接口拒绝等场景；这些值用于响应语义，不是持久化
 *     的购物车状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum CartCodeEnum implements BaseEnum {
  CART_ITEM_NOT_FOUND("14001", "CART_ITEM_NOT_FOUND", "购物车项不存在"),
  MOVE_TO_FAVORITE_FAIL("14002", "MOVE_TO_FAVORITE_FAIL", "移入收藏夹失败"),
  CART_ITEM_NOT_SELECTED("14003", "CART_ITEM_NOT_SELECTED", "购物车项未勾选"),
  CART_ITEM_MISMATCH("14004", "CART_ITEM_MISMATCH", "购物车项与结算快照不一致"),
  CART_CHECKOUT_CLEANUP_FAIL("14005", "CART_CHECKOUT_CLEANUP_FAIL", "购物车结算清理失败"),
  INTERNAL_ACCESS_DENIED("14011", "INTERNAL_ACCESS_DENIED", "内部接口访问被拒绝");

  private final String code;
  private final String alias;
  private final String message;
}
