package com.yirancrazy.minimall.notify.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
* 通知 C 端接口控制器，提供按用户拉取消息列表能力，供前端消息中心展示。
 */
@RestController
@RequestMapping("/api/v1/notify")
public class NotifyControllerV1 {

    private final NotifyService notifyService;

    public NotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 拉取指定用户的消息列表，按持久化顺序返回全部通知记录。
     *
     * @param dto 查询条件
     * @return 通知消息列表的 Result 包装
     */
    @GetMapping
    public Result<List<NotifyMessagePO>> list(@Valid NotifyListDTO dto) {
        return Result.success(notifyService.listByUser(dto));
    }
}