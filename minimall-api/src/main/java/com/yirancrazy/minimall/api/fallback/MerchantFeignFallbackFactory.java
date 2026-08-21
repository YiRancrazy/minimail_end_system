package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantFeign Feign 降级工厂，处理MerchantFeign服务调用失败降级
 * @Version: 1.2
 * @DateTime: 2026/08/10
 */
@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service unreachable: {}", cause.getMessage());
        return new MerchantFeignClient() {
            @Override
            public Result<ShopSnapshotDTO> shopSnapshot(Long id) {
                // 哨兵值：shopId/merchantId=-1 且 status=DOWN，下游校验必然失败，确保降级时拒绝发布而非放行
                return Result.success(new ShopSnapshotDTO(-1L, -1L, "unknown", "DOWN"));
            }

            @Override
            public Result<CursorPageVO<MerchantManageVO>> pageManage(InternalPageQuery query) {
                int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
                return Result.success(CursorPageVO.empty(limit));
            }

            @Override
            public Result<MerchantManageVO> detail(Long merchantId) {
                return Result.success(null);
            }

            @Override
            public Result<Void> audit(Long merchantId, boolean approved, String reason) {
                log.warn("merchant audit fallback, merchantId={}, approved={} skipped", merchantId, approved);
                return Result.fail(CommonCode.SYS_ERROR, "商家服务不可用");
            }
        };
    }
}
