package com.yirancrazy.minimall.goods.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SPU 草稿、待审、在售、下架和驳回状态码及展示文案。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum SpuStatusEnum implements BaseEnum {
    DRAFT("0", "DRAFT", "草稿"),
    PENDING_AUDIT("1", "PENDING_AUDIT", "待审"),
    ON_SALE("2", "ON_SALE", "在售"),
    OFF_SHELF("3", "OFF_SHELF", "下架"),
    REJECTED("4", "REJECTED", "审核驳回");

    private final String code;
    private final String alias;
    private final String message;

    /**
     * 将 SPU 状态的字符串编码转换为数据库查询使用的整数状态值。
     *
     * @return 当前 SPU 状态对应的整数编码
     */
    public int statusValue() {
        return Integer.parseInt(code);
    }
}
