package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.service.NotifyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 暴露通知模块内部写入接口，接收其他服务提交的用户通知事件；路径位于 /internal/notify，不应直接暴露给公网.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/notify")
public class InternalNotifyControllerV1 {

    private final NotifyService notifyService;

    /**
     * 注入站内通知业务服务.
     *
     * @param notifyService 通知写入入口
     */
    public InternalNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 将内部事件中的用户、标题和正文写入站内通知，不携带业务幂等键.
     *
     * @param dto 内部通知事件，包含接收用户和消息内容
     * @return true 表示通知成功落库，false 表示保存失败
     */
    @PostMapping("/push")
    public Result<Boolean> push(@Valid @RequestBody NotifyEventDto dto) {
        boolean saved = notifyService.push(dto.getUserId(), dto.getTitle(), dto.getContent());
        return Result.success(saved);
    }
}
