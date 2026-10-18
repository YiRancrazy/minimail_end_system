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
 * @Description: MerchantFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service 不可达: {}", cause.getMessage());
        return new MerchantFeignClient() {
            /**
             * 执行 shopSnapshot 方法。
             * @param id id 参数
             * @return 统一响应结果
             */
            @Override
            public Result<ShopSnapshotDTO> shopSnapshot(Long id) {
                // 哨兵值：shopId/merchantId=-1 且 status=DOWN，下游校验必然失败，确保降级时拒绝发布而非放行
                return Result.success(new ShopSnapshotDTO(-1L, -1L, "unknown", "DOWN"));
            }

            /**
             * 分页查询数据。
             * @param query query 参数
             * @return 统一响应结果
             */
            @Override
            public Result<CursorPageVO<MerchantManageVO>> pageManage(InternalPageQuery query) {
                int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
                return Result.success(CursorPageVO.empty(limit));
            }

            /**
             * 执行 detail 方法。
             * @param merchantId merchantId 参数
             * @return 统一响应结果
             */
            @Override
            public Result<MerchantManageVO> detail(Long merchantId) {
                return Result.success(null);
            }

            /**
             * 执行 audit 方法。
             * @param merchantId merchantId 参数
             * @param approved approved 参数
             * @param reason reason 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> audit(Long merchantId, boolean approved, String reason) {
                log.warn("商家审核降级，merchantId={}，approved={}，已跳过", merchantId, approved);
                return Result.fail(CommonCode.SYS_ERROR, "商家服务不可用");
            }
        };
    }
}
