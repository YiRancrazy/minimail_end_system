package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.api.dto.user.UserProvisionDto;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户建档、身份快照、收藏和平台用户管理远程调用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-user-service", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 根据认证用户 ID 创建对应业务用户档案.
     *
     * @param dto 认证侧用户 ID
     * @return 建档成功或系统错误
     */
    @PostMapping("/internal/user/provision")
    Result<Void> provision(@RequestBody UserProvisionDto dto);

    /**
     * 查询用户昵称与角色快照，供其他服务展示或鉴权使用.
     *
     * @param id 用户 ID
     * @return 包装后的用户身份快照
     */
    @GetMapping("/internal/user/{id}")
    Result<UserSnapshotDto> snapshot(@PathVariable("id") Long id);

    /**
     * 为指定用户收藏一个 SKU.
     *
     * @param userId 用户 ID
     * @param skuId 被收藏的 SKU ID
     * @return 收藏成功或系统错误
     */
    @PostMapping("/internal/favorites/{userId}/{skuId}")
    Result<Void> addFavorite(@PathVariable("userId") Long userId, @PathVariable("skuId") Long skuId);

    /**
     * 按游标和筛选条件分页查询平台用户管理视图.
     *
     * @param query 游标、数量、关键字和状态筛选条件
     * @return 包装后的用户游标分页结果
     */
    @GetMapping("/internal/user/manage")
    Result<CursorPageVo<UserManageVo>> pageManage(@SpringQueryMap InternalPageQuery query);

    /**
     * 查询指定用户的平台管理详情.
     *
     * @param userId 用户 ID
     * @return 包装后的用户管理视图
     */
    @GetMapping("/internal/user/manage/{userId}")
    Result<UserManageVo> detail(@PathVariable("userId") Long userId);
}
