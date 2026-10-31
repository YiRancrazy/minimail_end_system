package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理服务健康检查接口，用于确认服务进程可响应请求.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform")
public class HealthControllerV1 {

  /**
   * 返回平台管理服务的固定健康标识，不访问数据库或下游服务.
   *
   * @return 成功响应，数据固定为 platform-ok
   */
  @GetMapping("/health")
  public Result<String> health() {
    return Result.success("platform-ok");
  }
}
