package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDto;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在通知服务不可用时将推送结果降级为未发送.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class NotifyFeignFallbackFactory implements FallbackFactory<NotifyFeignClient> {
    /**
     * 为通知远程调用异常创建降级客户端.
     *
     * @param cause 通知远程调用异常
     * @return 通知降级客户端
     */
    @Override
    public NotifyFeignClient create(Throwable cause) {
        log.warn("notify-service 不可达: {}", cause.getMessage());
        return new NotifyFeignClient() {
            /**
             * 通知服务不可用时返回 false，表示通知未受理.
             *
             * @param dto 通知事件
             * @return 数据为 false 的成功响应
             */
            @Override
            public Result<Boolean> push(NotifyEventDto dto) {
                return Result.success(false);
            }
        };
    }
}
