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
 * @Description: PlatformUserControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/users")
public class PlatformUserControllerV1 {

    private final PlatformUserService platformUserService;

    /**
     * 构造 PlatformUserControllerV1 实例。
     * @param platformUserService platformUserService 参数
     */
    public PlatformUserControllerV1(PlatformUserService platformUserService) {
        this.platformUserService = platformUserService;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<PlatformUserVO>> page(@Valid PlatformUserPageDTO dto) {
        return Result.success(platformUserService.page(dto));
    }


    /**
     * 执行 detail 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{userId}")
    public Result<PlatformUserVO> detail(@PathVariable("userId") Long userId) {
        return Result.success(platformUserService.detail(userId));
    }
}
