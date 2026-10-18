package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalUserControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/user")
public class InternalUserControllerV1 {

    private final UserService userService;

    /**
     * 构造 InternalUserControllerV1 实例。
     * @param userService userService 参数
     */
    public InternalUserControllerV1(UserService userService) {
        this.userService = userService;
    }


    /**
     * 执行 snapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<UserSnapshotDTO> snapshot(@PathVariable Long id) {
        return Result.success(userService.getSnapshot(id));
    }
}
