package com.yirancrazy.minimall.platform.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家列表分页查询入参，支持按名称/审核状态过滤
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformMerchantPageDTO extends CursorPageDTO {

    /** 按商家名称模糊匹配 */
    private String keyword;

    /** 按审核状态过滤：0=待审 1=通过 2=驳回 */
    private Integer auditStatus;
}
