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
    private String shopName;
    private String licenseNo;
    private String status;
}