package com.yirancrazy.minimall.notify.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum ComplaintStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待处理"),
    PROCESSING(1, "PROCESSING", "处理中"),
    RESOLVED(2, "RESOLVED", "已解决"),
    REJECTED(3, "REJECTED", "已驳回");

    private final int code;
    private final String alias;
    private final String message;

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 intCode 方法。
     * @return 业务数值
     */
    public int intCode() {
        return code;
    }
}
