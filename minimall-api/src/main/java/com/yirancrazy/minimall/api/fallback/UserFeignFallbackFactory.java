package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("user-service 不可达: {}", cause.getMessage());
        return new UserFeignClient() {
            /**
             * 执行 snapshot 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<UserSnapshotDTO> snapshot(Long id) {
                return Result.success(new UserSnapshotDTO(-1L, "unknown", "ANONYMOUS"));
            }

            /**
             * 执行 addFavorite 方法。
             * @param userId userId 参数
             * @param skuId skuId 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> addFavorite(Long userId, Long skuId) {
                return Result.fail(CommonCode.SYS_ERROR, "user-service 不可达");
            }

            /**
             * 分页查询数据。
             * @param query query 参数
             * @return 统一响应结果
             */
            @Override
            public Result<CursorPageVO<UserManageVO>> pageManage(InternalPageQuery query) {
                int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
                return Result.success(CursorPageVO.empty(limit));
            }

            /**
             * 执行 detail 方法。
             * @param userId userId 参数
             * @return 统一响应结果
             */
            @Override
            public Result<UserManageVO> detail(Long userId) {
                return Result.success(null);
            }
        };
    }
}
