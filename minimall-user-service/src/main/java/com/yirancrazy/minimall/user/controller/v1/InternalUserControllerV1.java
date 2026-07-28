package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/user")
public class InternalUserControllerV1 {

    private final UserService userService;

    public InternalUserControllerV1(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Result<UserSnapshotDTO> snapshot(@PathVariable Long id) {
        UserPO u = userService.getById(id);
        return Result.success(new UserSnapshotDTO(u.getId(), u.getUsername(), "USER"));
    }
}