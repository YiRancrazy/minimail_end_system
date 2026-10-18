package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.service.NotifyService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalNotifyControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/notify")
public class InternalNotifyControllerV1 {

    private final NotifyService notifyService;

    /**
     * 构造 InternalNotifyControllerV1 实例。
     * @param notifyService notifyService 参数
     */
    public InternalNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    /**
     * 执行 push 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/push")
    public Result<Boolean> push(@Valid @RequestBody NotifyEventDTO dto) {
        boolean saved = notifyService.push(dto.getUserId(), dto.getTitle(), dto.getContent());
        return Result.success(saved);
    }
}
