package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDto;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向通知服务提交站内通知事件.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface NotifyFeignClient {

  /**
   * 向指定用户投递站内通知事件.
   *
   * @param dto 通知接收人、标题和正文
   * @return 包装后的通知受理结果
   */
  @PostMapping("/internal/notify/push")
  Result<Boolean> push(@RequestBody NotifyEventDto dto);
}
