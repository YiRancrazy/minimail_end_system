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
 * @Description: MerchantFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-merchant-service", fallbackFactory = MerchantFeignFallbackFactory.class)
public interface MerchantFeignClient {


    /**
     * 执行 shopSnapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/merchant/shop/{id}")
    Result<ShopSnapshotDTO> shopSnapshot(@PathVariable("id") Long id);


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/merchant/manage")
    Result<CursorPageVO<MerchantManageVO>> pageManage(@SpringQueryMap InternalPageQuery query);


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/merchant/manage/{merchantId}")
    Result<MerchantManageVO> detail(@PathVariable("merchantId") Long merchantId);


    /**
     * 执行 audit 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/merchant/manage/{merchantId}/audit")
    Result<Void> audit(@PathVariable("merchantId") Long merchantId,
                       @RequestParam boolean approved,
                       @RequestParam(required = false) String reason);
}
