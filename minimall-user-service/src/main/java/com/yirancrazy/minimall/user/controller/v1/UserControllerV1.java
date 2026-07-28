package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserControllerV1 {

    private final UserService userService;

    public UserControllerV1(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Result<UserPO> get(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@RequestBody UserPO user) {
        return Result.success(userService.create(user));
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody UserPO user) {
        return Result.success(userService.update(id, user));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(userService.delete(id));
    }
}