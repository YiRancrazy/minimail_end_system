package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Shop持久化对象，映射shop表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class ShopPO extends BasePO {
    private String shopName;
    private String licenseNo;
    private String status;
}