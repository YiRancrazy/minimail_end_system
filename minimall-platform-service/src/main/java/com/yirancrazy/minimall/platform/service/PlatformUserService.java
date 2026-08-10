package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户服务接口；当前契约由 user-service 提供，本端作为契约入口。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
public interface PlatformUserService {

    /**
     * 分页查询用户列表，支持按用户名/昵称过滤。
     * @param dto 分页入参
     * @return 用户游标分页结果
     */
    CursorPageVO<PlatformUserVO> page(PlatformUserPageDTO dto);

    /**
     * 查询用户详情，不存在时抛 PLATFORM_USER_NOT_FOUND。
     * @param userId 用户ID
     * @return 用户视图
     */
    PlatformUserVO detail(Long userId);
}
