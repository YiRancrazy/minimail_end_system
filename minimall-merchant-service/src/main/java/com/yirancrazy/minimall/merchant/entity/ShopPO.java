package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 店铺持久化实体，对应 t_shop 表，承载店铺名称、营业执照号与状态等基础字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_shop")
public class ShopPO extends BasePO {
    private String shopName;
    private String licenseNo;
    private String status;
}