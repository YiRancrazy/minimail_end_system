package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Shop持久化对象，映射t_merch_shop表
 * @Version: 1.1
 * @DateTime: 2026/08/04
 */
@Data
@TableName("t_merch_shop")
public class ShopPO extends BasePO {
    /**
     * 归属商家ID，越权校验依据（商家只能访问自己名下的店铺）。
     */
    private Long merchantId;
    private String shopName;
    private String licenseNo;
    // 状态: 1-营业中, 2-已停业, 3-已冻结（对应 ShopStatusEnum int code）
    private Integer status;
}