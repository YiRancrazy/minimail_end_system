package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.MerchantFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Merchant Feign 客户端，调用Merchant服务接口
 * @Version: 1.3
 * @DateTime: 2026/08/10
 */
@FeignClient(name = "minimall-merchant-service", fallbackFactory = MerchantFeignFallbackFactory.class)
public interface MerchantFeignClient {

    /**
     * 查询店铺快照信息。
     * @param id 店铺ID
     * @return 店铺快照；服务不可用时由 fallback 返回哨兵值
     */
    @GetMapping("/internal/merchant/shop/{id}")
    Result<ShopSnapshotDTO> shopSnapshot(@PathVariable("id") Long id);

    /**
     * 平台商家管理分页查询。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param query 通用分页查询
     * @return 商家管理分页结果；服务不可用时由 fallback 返回空分页
     */
    @GetMapping("/internal/merchant/manage")
    Result<CursorPageVO<MerchantManageVO>> pageManage(@SpringQueryMap InternalPageQuery query);

    /**
     * 平台商家详情。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param merchantId 商家ID
     * @return 商家管理视图；服务不可用时由 fallback 返回 null
     */
    @GetMapping("/internal/merchant/manage/{merchantId}")
    Result<MerchantManageVO> detail(@PathVariable("merchantId") Long merchantId);

    /**
     * 平台审核商家资质，仅允许 PENDING 状态审核；驳回时 reason 必填。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param merchantId 商家主体ID
     * @param approved 是否通过
     * @param reason 驳回原因，approved=false 时必填
     * @return 空成功响应；服务不可用时由 fallback 返回 SYS_ERROR 失败结果
     */
    @PostMapping("/internal/merchant/manage/{merchantId}/audit")
    Result<Void> audit(@PathVariable("merchantId") Long merchantId,
                       @RequestParam boolean approved,
                       @RequestParam(required = false) String reason);
}
