package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notify")
public class InternalNotifyControllerV1 {

    private final NotifyService notifyService;
    private final SseHub sseHub;

    public InternalNotifyControllerV1(NotifyService notifyService, SseHub sseHub) {
        this.notifyService = notifyService;
        this.sseHub = sseHub;
    }

    @PostMapping("/push")
    public Result<Boolean> push(@RequestBody NotifyEventDTO dto) {
        boolean saved = notifyService.push(dto.getUserId(), dto.getTitle(), dto.getContent());
        sseHub.send(dto.getUserId(), dto.getTitle() + ": " + dto.getContent());
        return Result.success(saved);
    }
}