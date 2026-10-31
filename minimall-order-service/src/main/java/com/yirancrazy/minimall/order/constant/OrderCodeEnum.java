package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 集中定义订单模块的业务错误码、稳定别名和面向用户的错误消息。
 *   业务边界：编码用于 BizException 与接口响应，不承担异常抛出或状态流转职责。
 *   使用场景：订单创建、库存预占、支付、退款和发件箱异常场景统一引用该枚举。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum OrderCodeEnum implements BaseEnum {
  STOCK_RESERVE_FAIL("11001", "STOCK_RESERVE_FAIL", "库存锁定失败"),
  ORDER_NOT_FOUND("11002", "ORDER_NOT_FOUND", "订单不存在"),
  PAY_FAIL("11003", "PAY_FAIL", "支付失败"),
  ORDER_SKU_SNAPSHOT_MISSING("11004", "ORDER_SKU_SNAPSHOT_MISSING", "商品快照缺失"),
  ORDER_PAY_CREATE_FAIL("11005", "ORDER_PAY_CREATE_FAIL", "创建支付流水失败"),
  ORDER_STATUS_TRANSITION_INVALID("50001", "ORDER_STATUS_TRANSITION_INVALID", "订单状态流转不合法"),
  ORDER_ALREADY_CANCELLED("50002", "ORDER_ALREADY_CANCELLED", "订单已取消"),
  ORDER_DELETE_NOT_ALLOWED("50003", "ORDER_DELETE_NOT_ALLOWED", "订单状态不允许删除"),
  ORDER_NOT_REFUNDING("50004", "ORDER_NOT_REFUNDING", "订单非退款中状态"),
  ORDER_ITEMS_EMPTY("50005", "ORDER_ITEMS_EMPTY", "结算商品列表不能为空"),
  REFUND_AMOUNT_INVALID("50006", "REFUND_AMOUNT_INVALID", "退款金额非法"),
  ORDER_REFUND_FAIL("50007", "ORDER_REFUND_FAIL", "订单退款失败"),
  ORDER_AMOUNT_SCALE_INVALID("50008", "ORDER_AMOUNT_SCALE_INVALID", "订单金额最多支持两位小数"),
  ORDER_ID_GENERATE_FAIL("50009", "ORDER_ID_GENERATE_FAIL", "全局ID生成失败"),
  ORDER_ITEMS_LIMIT_EXCEEDED("50010", "ORDER_ITEMS_LIMIT_EXCEEDED", "单次结算最多100个SKU"),
  ORDER_CHECKOUT_ITEM_INVALID("50011", "ORDER_CHECKOUT_ITEM_INVALID", "结算明细非法"),
  ORDER_MULTI_MERCHANT_NOT_ALLOWED("50012", "ORDER_MULTI_MERCHANT_NOT_ALLOWED", "仅支持同一商家下单"),
  ORDER_OUTBOX_SAVE_FAILED("50013", "ORDER_OUTBOX_SAVE_FAILED", "订单发件箱保存失败");

  private final String code;
  private final String alias;
  private final String message;
}
