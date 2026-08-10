package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户控制器，提供用户列表与详情接口。
 *              当前阶段数据由 user-service 提供，本端作为契约入口。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/platform/users")
public class PlatformUserControllerV1 {

    private final PlatformUserService platformUserService;

    public PlatformUserControllerV1(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }

    /**
     * 平台分页查询用户列表。
     * @param dto 分页入参
     * @return 用户游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<PlatformUserVO>> page(@Valid PlatformUserPageDTO dto) {
        return Result.success(platformUserService.page(dto));
    }

    /**
     * 平台查询用户详情。
     * @param userId 用户ID
     * @return 用户视图
     */
    @GetMapping("/{userId}")
    public Result<PlatformUserVO> detail(@PathVariable("userId") Long userId) {
        return Result.success(platformUserService.detail(userId));
    }
}
