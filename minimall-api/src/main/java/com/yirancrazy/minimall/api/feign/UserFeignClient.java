package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: User Feign 客户端，调用User服务接口
 * @Version: 1.2
 * @DateTime: 2026/08/03
 */
@FeignClient(name = "minimall-user-service", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {
    /**
     * 查询用户快照信息。
     * @param id 用户ID
     * @return 用户快照；服务不可用时由 fallback 返回哨兵值
     */
    @GetMapping("/internal/user/{id}")
    Result<UserSnapshotDTO> snapshot(@PathVariable("id") Long id);

    /**
     * 收藏指定商品，供跨服务移入收藏夹调用。
     * @param userId 用户ID
     * @param skuId 商品SKU ID
     * @return 无业务数据；服务不可用时由 fallback 返回失败响应
     */
    @PostMapping("/internal/favorites/{userId}/{skuId}")
    Result<Void> addFavorite(@PathVariable("userId") Long userId, @PathVariable("skuId") Long skuId);
}
