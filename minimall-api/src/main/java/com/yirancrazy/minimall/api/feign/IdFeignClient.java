package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Id Feign 客户端，调用Id服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface IdFeignClient {
    /**
     * 根据业务标签获取下一个全局唯一 ID。
     * @param bizTag 业务标签
     * @return 全局唯一 ID；服务不可用时由 fallback 返回 -1
     */
    @GetMapping("/internal/id/next")
    Result<Long> nextId(@RequestParam("bizTag") String bizTag);
}
