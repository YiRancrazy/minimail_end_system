package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在 ID 服务不可用时返回系统错误，阻止调用方使用无效 ID.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
  /**
   * 为 ID 远程调用异常创建降级客户端.
   *
   * @param cause ID 远程调用异常
   * @return ID 降级客户端
   */
  @Override
  public IdFeignClient create(Throwable cause) {
    log.warn("id-service 不可达: {}", cause.getMessage());
    return new IdFeignClient() {
      /**
       * ID 服务不可用时返回系统错误，避免将 -1 当作合法 ID 落库.
       *
       * @param bizTag 业务标识
       * @return 系统错误响应
       */
      @Override
      public Result<Long> nextId(String bizTag) {
        // 不可用即返回系统错误，避免调用方把 -1 当合法 ID 落库
        return Result.fail(CommonCode.SYS_ERROR, "ID服务不可用");
      }
    };
  }
}
