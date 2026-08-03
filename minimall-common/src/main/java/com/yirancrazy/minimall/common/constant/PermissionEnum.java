package com.yirancrazy.minimall.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统细粒度权限枚举，用于 @RequirePermission 注解声明接口所需权限。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Getter
@AllArgsConstructor
public enum PermissionEnum {

    // ---- 用户端权限 ----
    GOODS_VIEW("GOODS_VIEW", "浏览商品"),
    CART_MANAGE("CART_MANAGE", "管理购物车"),
    ORDER_CREATE("ORDER_CREATE", "创建订单"),
    ORDER_VIEW("ORDER_VIEW", "查看订单"),
    ORDER_CANCEL("ORDER_CANCEL", "取消订单"),
    PAY_VIEW("PAY_VIEW", "查看支付"),
    NOTIFY_VIEW("NOTIFY_VIEW", "查看消息"),
    PROFILE_MANAGE("PROFILE_MANAGE", "管理个人资料"),

    // ---- 商家端权限 ----
    GOODS_MANAGE("GOODS_MANAGE", "管理商品"),
    STOCK_MANAGE("STOCK_MANAGE", "管理库存"),
    ORDER_SHIP("ORDER_SHIP", "订单发货"),
    QUALIFICATION_SUBMIT("QUALIFICATION_SUBMIT", "提交资质"),

    // ---- 平台端权限 ----
    MERCHANT_AUDIT("MERCHANT_AUDIT", "审核商家"),
    GOODS_AUDIT("GOODS_AUDIT", "审核商品"),
    ORDER_VIEW_ALL("ORDER_VIEW_ALL", "查看全平台订单"),
    ORDER_STATISTICS("ORDER_STATISTICS", "查看订单统计"),
    ORDER_DELETE("ORDER_DELETE", "删除订单"),
    STOCK_VIEW_ALL("STOCK_VIEW_ALL", "查看全平台库存"),
    PAY_MANAGE("PAY_MANAGE", "管理支付"),
    REFUND_AUDIT("REFUND_AUDIT", "退款审核"),
    ROLE_VIEW("ROLE_VIEW", "查看角色权限"),
    NOTIFY_BROADCAST("NOTIFY_BROADCAST", "广播消息"),
    PLATFORM_FINANCE("PLATFORM_FINANCE", "财务对账");

    private final String code;
    private final String description;
}
