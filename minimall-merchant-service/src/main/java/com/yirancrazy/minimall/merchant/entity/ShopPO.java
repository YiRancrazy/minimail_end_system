package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopPO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class ShopPO extends BasePO {
    private String shopName;
    private String licenseNo;
    private String status;
}