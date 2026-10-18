package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-id-service", fallbackFactory = IdFeignFallbackFactory.class)
public interface IdFeignClient {

    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/id/next")
    Result<Long> nextId(@RequestParam("bizTag") String bizTag);
}
