package com.yirancrazy.minimall.notify.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台系统告警列表的游标分页条件，不包含额外业务筛选字段.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemAlertPageDto extends CursorPageDto {}
