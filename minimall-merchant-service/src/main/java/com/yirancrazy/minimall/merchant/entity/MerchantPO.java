package com.yirancrazy.minimall.merchant.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家主体持久化对象，映射 t_merch_merchant 表，含资质审核状态。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merch_merchant")
public class MerchantPO extends BasePO {
    private Long userId;
    private String merchantName;
    private String licenseNo;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditAt;
}
