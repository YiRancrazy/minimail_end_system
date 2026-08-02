package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Notify Feign 客户端，调用Notify服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface NotifyFeignClient {
    /**
     * 推送通知事件。
     * @param dto 通知事件负载
     * @return 推送是否成功；服务不可用时由 fallback 返回 false
     */
    @PostMapping("/internal/notify/push")
    Result<Boolean> push(@RequestBody NotifyEventDTO dto);
}
