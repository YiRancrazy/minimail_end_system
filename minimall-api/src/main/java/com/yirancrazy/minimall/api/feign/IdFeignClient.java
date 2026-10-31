package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向各业务服务提供按业务标签生成的分布式唯一 ID.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-id-service", fallbackFactory = IdFeignFallbackFactory.class)
public interface IdFeignClient {

  /**
   * 按业务标签生成全局唯一 ID.
   *
   * @param bizTag 业务标识，例如订单或用户
   * @return 包装后的分布式唯一 ID
   */
  @GetMapping("/internal/id/next")
  Result<Long> nextId(@RequestParam("bizTag") String bizTag);
}
