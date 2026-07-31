package com.yirancrazy.minimall.user.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalUserControllerV1 description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class InternalUserControllerV1 {

    private final UserService userService;

    public InternalUserControllerV1(UserService userService) {
        this.userService = userService;
    }

    /**
     * 根据用户 ID 查询并组装供其他服务使用的用户快照。
     *
     * @param id 用户唯一标识
     * @return 用户内部快照
     */
    @GetMapping("/{id}")
    public Result<UserSnapshotDTO> snapshot(@PathVariable Long id) {
        UserPO u = userService.getById(id);
        return Result.success(new UserSnapshotDTO(u.getId(), u.getUsername(), "USER"));
    }
}