package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/merchant/shop")
public class InternalShopControllerV1 {

    private final ShopService shopService;

    public InternalShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/{id}")
    public Result<ShopSnapshotDTO> snapshot(@PathVariable Long id) {
        ShopPO s = shopService.getById(id);
        return Result.success(new ShopSnapshotDTO(s.getId(), s.getShopName(), s.getStatus()));
    }
}