package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.service.ShopService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalShopControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/merchant/shop")
public class InternalShopControllerV1 {

    private final ShopService shopService;

    /**
     * 构造 InternalShopControllerV1 实例。
     * @param shopService shopService 参数
     */
    public InternalShopControllerV1(ShopService shopService) {
        this.shopService = shopService;
    }


    /**
     * 执行 snapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<ShopSnapshotDTO> snapshot(@PathVariable Long id) {
        return Result.success(shopService.getSnapshot(id));
    }
}
