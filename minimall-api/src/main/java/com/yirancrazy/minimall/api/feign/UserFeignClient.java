package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: User Feign 客户端，调用User服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/10
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

    /**
     * 平台用户管理分页查询。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param query 通用分页查询
     * @return 用户管理分页结果；服务不可用时由 fallback 返回空分页
     */
    @GetMapping("/internal/user/manage")
    Result<CursorPageVO<UserManageVO>> pageManage(@SpringQueryMap InternalPageQuery query);

    /**
     * 平台用户详情。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param userId 用户ID
     * @return 用户管理视图；服务不可用时由 fallback 返回 null
     */
    @GetMapping("/internal/user/manage/{userId}")
    Result<UserManageVO> detail(@PathVariable("userId") Long userId);
}
