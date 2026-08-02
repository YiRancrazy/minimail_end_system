package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Merchant Feign 客户端，调用Merchant服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface MerchantFeignClient {
    /**
     * 查询店铺快照信息。
     * @param id 店铺ID
     * @return 店铺快照；服务不可用时由 fallback 返回哨兵值
     */
    @GetMapping("/internal/merchant/shop/{id}")
    Result<ShopSnapshotDTO> shopSnapshot(@PathVariable("id") Long id);
}
