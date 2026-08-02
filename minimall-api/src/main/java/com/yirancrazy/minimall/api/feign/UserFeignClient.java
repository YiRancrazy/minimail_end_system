package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: User Feign 客户端，调用User服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface UserFeignClient {
    /**
     * 查询用户快照信息。
     * @param id 用户ID
     * @return 用户快照；服务不可用时由 fallback 返回哨兵值
     */
    @GetMapping("/internal/user/{id}")
    Result<UserSnapshotDTO> snapshot(@PathVariable("id") Long id);
}
