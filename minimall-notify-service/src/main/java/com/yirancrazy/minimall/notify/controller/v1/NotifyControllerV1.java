package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notify")
public class NotifyControllerV1 {

    private final NotifyService notifyService;

    public NotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @GetMapping
    public Result<List<NotifyMessagePO>> list(@RequestParam("userId") Long userId) {
        return Result.success(notifyService.listByUser(userId));
    }
}