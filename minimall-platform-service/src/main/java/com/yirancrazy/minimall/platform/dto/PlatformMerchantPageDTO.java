package com.yirancrazy.minimall.platform.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家管理游标分页条件，支持关键词和审核状态筛选.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformMerchantPageDto extends CursorPageDto {

  private String keyword;

  private Integer auditStatus;
}
