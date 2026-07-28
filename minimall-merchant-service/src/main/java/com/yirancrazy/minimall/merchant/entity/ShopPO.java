package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shop")
public class ShopPO extends BasePO {
    private String shopName;
    private String licenseNo;
    private String status;
}