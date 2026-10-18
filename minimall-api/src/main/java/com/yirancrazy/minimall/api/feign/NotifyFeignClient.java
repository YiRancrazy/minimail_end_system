package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface NotifyFeignClient {

    /**
     * 执行 push 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/notify/push")
    Result<Boolean> push(@RequestBody NotifyEventDTO dto);
}
