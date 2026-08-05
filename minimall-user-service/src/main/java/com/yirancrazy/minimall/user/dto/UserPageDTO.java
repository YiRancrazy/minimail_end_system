package com.yirancrazy.minimall.user.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户分页查询入参，支持按用户名/昵称模糊搜索
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageDTO extends CursorPageDTO {

    private String keyword;
}
