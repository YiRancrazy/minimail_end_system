package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeign Feign 降级工厂，处理IdFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
    @Override
    public IdFeignClient create(Throwable cause) {
        log.warn("id-service unreachable: {}", cause.getMessage());
        return new IdFeignClient() {
            @Override
            public Result<Long> nextId(String bizTag) {
                // 不可用即返回系统错误，避免调用方把 -1 当合法 ID 落库
                return Result.fail(CommonCode.SYS_ERROR, "ID服务不可用");
            }
        };
    }
}
