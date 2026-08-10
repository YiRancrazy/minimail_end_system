package com.yirancrazy.minimall.merchant.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家管理分页查询入参，支持按名称/审核状态过滤。
 *              仅供内部 InternalMerchantManageControllerV1 使用，不暴露给商家端。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantPageDTO extends CursorPageDTO {

    /** 按商家名称模糊匹配 */
    private String keyword;

    /** 按审核状态过滤：0=待审 1=通过 2=驳回 */
    private Integer auditStatus;
}
