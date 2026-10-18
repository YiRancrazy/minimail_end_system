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
 * @Description: UserFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-user-service", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 执行 snapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/user/{id}")
    Result<UserSnapshotDTO> snapshot(@PathVariable("id") Long id);


    /**
     * 执行 addFavorite 方法。
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/favorites/{userId}/{skuId}")
    Result<Void> addFavorite(@PathVariable("userId") Long userId, @PathVariable("skuId") Long skuId);


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/user/manage")
    Result<CursorPageVO<UserManageVO>> pageManage(@SpringQueryMap InternalPageQuery query);


    /**
     * 执行 detail 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/user/manage/{userId}")
    Result<UserManageVO> detail(@PathVariable("userId") Long userId);
}
