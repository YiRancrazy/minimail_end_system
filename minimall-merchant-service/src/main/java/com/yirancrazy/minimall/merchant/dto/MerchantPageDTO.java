package com.yirancrazy.minimall.merchant.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质游标分页查询参数；携带游标、每页条数、商家名称关键字和审核状态，供内部管理端筛选商家记录。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPageDto extends CursorPageDto {

    private String keyword;

    private Integer auditStatus;
}
