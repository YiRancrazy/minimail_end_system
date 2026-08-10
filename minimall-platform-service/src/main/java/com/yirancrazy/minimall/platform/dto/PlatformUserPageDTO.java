package com.yirancrazy.minimall.platform.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户列表分页查询入参，支持按用户名/昵称过滤
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformUserPageDTO extends CursorPageDTO {

    /** 按用户名/昵称模糊匹配 */
    private String keyword;
}
