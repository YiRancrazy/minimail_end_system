package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Id Feign 客户端，调用Id服务接口
 * @Version: 1.2
 * @DateTime: 2026/08/02
 */
@FeignClient(name = "minimall-id-service", fallbackFactory = IdFeignFallbackFactory.class)
public interface IdFeignClient {
    /**
     * 根据业务标签获取下一个全局唯一 ID。
     * @param bizTag 业务标签
     * @return 全局唯一 ID；服务不可用时 fallback 返回 SYS_ERROR 失败结果（data 为 null）
     */
    @GetMapping("/internal/id/next")
    Result<Long> nextId(@RequestParam("bizTag") String bizTag);
}
