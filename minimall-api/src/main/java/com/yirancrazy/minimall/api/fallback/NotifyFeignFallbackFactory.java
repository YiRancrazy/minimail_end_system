package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class NotifyFeignFallbackFactory implements FallbackFactory<NotifyFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public NotifyFeignClient create(Throwable cause) {
        log.warn("notify-service 不可达: {}", cause.getMessage());
        return new NotifyFeignClient() {
            /**
             * 执行 push 方法。
             * @param dto dto 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Boolean> push(NotifyEventDTO dto) {
                return Result.success(false);
            }
        };
    }
}
