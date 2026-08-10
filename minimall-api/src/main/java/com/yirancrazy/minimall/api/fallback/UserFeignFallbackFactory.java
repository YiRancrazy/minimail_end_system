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
 * @Description: UserFeign Feign 降级工厂，处理UserFeign服务调用失败降级
 * @Version: 1.3
 * @DateTime: 2026/08/10
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("user-service unreachable: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<UserSnapshotDTO> snapshot(Long id) {
                return Result.success(new UserSnapshotDTO(-1L, "unknown", "ANONYMOUS"));
            }

            @Override
            public Result<Void> addFavorite(Long userId, Long skuId) {
                return Result.fail(CommonCode.SYS_ERROR, "user-service unreachable");
            }

            @Override
            public Result<CursorPageVO<UserManageVO>> pageManage(InternalPageQuery query) {
                int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
                return Result.success(CursorPageVO.empty(limit));
            }

            @Override
            public Result<UserManageVO> detail(Long userId) {
                return Result.success(null);
            }
        };
    }
}
