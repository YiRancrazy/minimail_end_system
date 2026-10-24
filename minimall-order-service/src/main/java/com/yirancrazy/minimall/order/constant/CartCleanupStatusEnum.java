package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理任务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
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
     * 获取字符串状态码。
     * @return 状态码
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 获取整数状态码。
     * @return 状态码
     */
    public int intCode() {
        return code;
    }
}
