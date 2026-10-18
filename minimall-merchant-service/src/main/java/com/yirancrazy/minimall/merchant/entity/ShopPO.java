package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@TableName("t_merch_shop")
public class ShopPO extends BasePO {

    private Long merchantId;
    private String shopName;
    private String licenseNo;
    // 状态: 1-营业中, 2-已停业, 3-已冻结（对应 ShopStatusEnum int code）
    private Integer status;
}