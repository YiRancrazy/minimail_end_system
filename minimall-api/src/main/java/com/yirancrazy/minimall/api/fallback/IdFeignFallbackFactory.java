package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public IdFeignClient create(Throwable cause) {
        log.warn("id-service 不可达: {}", cause.getMessage());
        return new IdFeignClient() {
            /**
             * 执行 nextId 方法。
             * @param bizTag bizTag 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Long> nextId(String bizTag) {
                // 不可用即返回系统错误，避免调用方把 -1 当合法 ID 落库
                return Result.fail(CommonCode.SYS_ERROR, "ID服务不可用");
            }
        };
    }
}
