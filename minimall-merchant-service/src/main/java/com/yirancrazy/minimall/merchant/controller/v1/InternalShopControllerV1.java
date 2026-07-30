package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* 店铺内部 RPC 接口控制器，供其他微服务通过 Feign 调用获取店铺快照数据。
 */
@RestController
@RequestMapping("/internal/merchant/shop")
public class InternalShopControllerV1 {

    private final ShopService shopService;

    public InternalShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 根据主键 ID 获取店铺快照信息，供其他微服务远程调用使用。
     *
     * @param id 店铺主键 ID
     * @return 店铺快照数据传输对象
     */
    @GetMapping("/{id}")
    public Result<ShopSnapshotDTO> snapshot(@PathVariable Long id) {
        ShopPO s = shopService.getById(id);
        return Result.success(new ShopSnapshotDTO(s.getId(), s.getShopName(), s.getStatus()));
    }
}