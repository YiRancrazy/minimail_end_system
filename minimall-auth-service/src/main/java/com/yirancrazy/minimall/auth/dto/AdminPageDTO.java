package com.yirancrazy.minimall.auth.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员游标分页查询入参；游标和每页数量由父类 CursorPageDto 提供。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPageDto extends CursorPageDto {}
