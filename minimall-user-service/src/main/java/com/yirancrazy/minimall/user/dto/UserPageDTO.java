package com.yirancrazy.minimall.user.dto;

import com.yirancrazy.minimall.common.dto.CursorPageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户列表的游标分页参数，支持按昵称关键字模糊筛选.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageDto extends CursorPageDto {

    private String keyword;
}
