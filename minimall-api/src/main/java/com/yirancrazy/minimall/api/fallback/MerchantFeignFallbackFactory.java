package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在商家服务不可用时返回拒绝发布的哨兵、空管理结果或明确失败.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class MerchantFeignFallbackFactory implements FallbackFactory<MerchantFeignClient> {
    /**
     * 为商家远程调用异常创建降级客户端.
     *
     * @param cause 商家远程调用异常
     * @return 商家降级客户端
     */
    @Override
    public MerchantFeignClient create(Throwable cause) {
        log.warn("merchant-service 不可达: {}", cause.getMessage());
        return new MerchantFeignClient() {
            /**
             * 商品服务不可达时返回不可营业的店铺哨兵，拒绝商品发布.
             *
             * @param id 店铺 ID
             * @return 商家和店铺 ID 均为 -1、状态为 DOWN 的快照
             */
            @Override
            public Result<ShopSnapshotDto> shopSnapshot(Long id) {
                // 哨兵值：shopId/merchantId=-1 且 status=DOWN，下游校验必然失败，确保降级时拒绝发布而非放行
                return Result.success(new ShopSnapshotDto(-1L, -1L, "unknown", "DOWN"));
            }

            /**
             * 商家服务不可用时返回保持请求数量上限的空分页.
             *
             * @param query 游标分页条件，可为空
             * @return 空商家分页结果
             */
            @Override
            public Result<CursorPageVo<MerchantManageVo>> pageManage(InternalPageQuery query) {
                int limit = query == null || query.getLimit() == null ? 20 : query.getLimit();
                return Result.success(CursorPageVo.empty(limit));
            }

            /**
             * 商家服务不可用时返回空管理详情.
             *
             * @param merchantId 商家 ID
             * @return 数据为空的成功响应
             */
            @Override
            public Result<MerchantManageVo> detail(Long merchantId) {
                return Result.success(null);
            }

            /**
             * 商家服务不可用时拒绝审核并通过日志保留审核上下文.
             *
             * @param merchantId 商家 ID
             * @param approved 是否通过审核
             * @param reason 驳回原因
             * @return 系统错误响应
             */
            @Override
            public Result<Void> audit(Long merchantId, boolean approved, String reason) {
                log.warn("商家审核降级，merchantId={}，approved={}，已跳过", merchantId, approved);
                return Result.fail(CommonCode.SYS_ERROR, "商家服务不可用");
            }
        };
    }
}
