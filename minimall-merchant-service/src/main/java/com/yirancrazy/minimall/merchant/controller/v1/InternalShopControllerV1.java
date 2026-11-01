package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部店铺快照接口；向服务间调用提供店铺 ID、商家 ID、名称和状态别名，路径限定在 /internal/merchant/shop。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/merchant/shop")
public class InternalShopControllerV1 {

    private final ShopService shopService;

    /**
     * 注入店铺领域服务。
     *
     * @param shopService 店铺领域服务
     */
    public InternalShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 读取店铺快照供其他服务组装业务数据。
     *
     * 内部调用不携带商家上下文，因此仅校验店铺存在，不校验商家归属。
     *
     * @param id 店铺主键
     * @return 统一响应，数据为店铺快照
     * @throws BizException 店铺不存在时抛出 SHOP_NOT_FOUND
     */
    @GetMapping("/{id}")
    public Result<ShopSnapshotDto> snapshot(@PathVariable Long id) {
        return Result.success(shopService.getSnapshot(id));
    }
}
