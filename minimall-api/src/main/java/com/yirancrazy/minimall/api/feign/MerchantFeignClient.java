package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.api.fallback.MerchantFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供店铺快照、平台商家分页、详情与资质审核远程调用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(
        name = "minimall-merchant-service",
        fallbackFactory = MerchantFeignFallbackFactory.class)
public interface MerchantFeignClient {

    /**
     * 查询店铺归属商家及当前营业状态，供商品发布校验.
     *
     * @param id 店铺 ID
     * @return 包装后的店铺快照
     */
    @GetMapping("/internal/merchant/shop/{id}")
    Result<ShopSnapshotDto> shopSnapshot(@PathVariable("id") Long id);

    /**
     * 按游标和筛选条件分页查询平台商家管理视图.
     *
     * @param query 游标、数量、关键字和审核状态筛选条件
     * @return 包装后的商家游标分页结果
     */
    @GetMapping("/internal/merchant/manage")
    Result<CursorPageVo<MerchantManageVo>> pageManage(@SpringQueryMap InternalPageQuery query);

    /**
     * 查询指定商家的平台管理详情和审核信息.
     *
     * @param merchantId 商家 ID
     * @return 包装后的商家管理视图
     */
    @GetMapping("/internal/merchant/manage/{merchantId}")
    Result<MerchantManageVo> detail(@PathVariable("merchantId") Long merchantId);

    /**
     * 审核商家资质，通过或驳回并记录驳回原因.
     *
     * @param merchantId 商家 ID
     * @param approved 是否通过审核
     * @param reason 驳回原因，通过时可为空
     * @return 审核成功或系统错误
     */
    @PostMapping("/internal/merchant/manage/{merchantId}/audit")
    Result<Void> audit(
            @PathVariable("merchantId") Long merchantId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason);
}
