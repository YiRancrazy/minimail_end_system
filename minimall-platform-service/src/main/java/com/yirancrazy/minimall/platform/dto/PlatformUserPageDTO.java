package com.yirancrazy.minimall.platform.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户管理游标分页条件，支持按关键词筛选.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformUserPageDto extends CursorPageDto {

    private String keyword;
}
