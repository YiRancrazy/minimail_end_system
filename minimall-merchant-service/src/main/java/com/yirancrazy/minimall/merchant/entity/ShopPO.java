package com.yirancrazy.minimall.merchant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺持久化对象，映射 t_merch_shop；关联商家 ID，保存店铺名称、证照号和 1/2/3 生命周期状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@TableName("t_merch_shop")
public class ShopPo extends BasePo {

    private Long merchantId;
    private String shopName;
    private String licenseNo;
    // 状态: 1-营业中, 2-已停业, 3-已冻结（对应 ShopStatusEnum int code）
    private Integer status;
}
