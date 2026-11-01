package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义购物车清理任务在待清理、成功和永久失败之间的持久化状态。
 *   业务边界：状态以整数 code 落库，alias 用于展示，message 用于描述；不表达远程购物车调用结果。
 *   使用场景：CartCleanupServiceImpl 和定时扫描任务据此筛选、领取和更新任务。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Getter
@AllArgsConstructor
public enum CartCleanupStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待清理"),
    SUCCESS(1, "SUCCESS", "已清理"),
    FAILED(2, "FAILED", "清理失败");

    private final int code;
    private final String alias;
    private final String message;

    /**
     * 获取字符串状态码.
     *
     * @return 状态码
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取整数状态码.
     *
     * @return 状态码
     */
    public int intCode() {
        return code;
    }
}
