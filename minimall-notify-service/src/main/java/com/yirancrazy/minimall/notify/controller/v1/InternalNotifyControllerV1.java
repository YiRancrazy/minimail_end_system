package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.sse.SseHub;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* 通知内部接口控制器，供他服务通过 HTTP 调用推送通知消息并触发 SSE。
 */
@RestController
@RequestMapping("/internal/notify")
public class InternalNotifyControllerV1 {

    private final NotifyService notifyService;
    private final SseHub sseHub;

    public InternalNotifyControllerV1(NotifyService notifyService, SseHub sseHub) {
        this.notifyService = notifyService;
        this.sseHub = sseHub;
    }

    /**
     * 持久化通知消息，并通过 SSE Hub 向在线用户的连接推送实时通知。
     *
     * @param dto 事件负载，包含目标用户、标题与内容
     * @return 持久化结果是否成功的 Result 包装
     */
    @PostMapping("/push")
    public Result<Boolean> push(@Valid @RequestBody NotifyEventDTO dto) {
        boolean saved = notifyService.push(dto.getUserId(), dto.getTitle(), dto.getContent());
        sseHub.send(dto.getUserId(), dto.getTitle() + ": " + dto.getContent());
        return Result.success(saved);
    }
}