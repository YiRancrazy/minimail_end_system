package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyFeign Feign 降级工厂，处理NotifyFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class NotifyFeignFallbackFactory implements FallbackFactory<NotifyFeignClient> {
    @Override
    public NotifyFeignClient create(Throwable cause) {
        log.warn("notify-service unreachable: {}", cause.getMessage());
        return new NotifyFeignClient() {
            @Override
            public Result<Boolean> push(NotifyEventDTO dto) {
                return Result.success(false);
            }
        };
    }
}
