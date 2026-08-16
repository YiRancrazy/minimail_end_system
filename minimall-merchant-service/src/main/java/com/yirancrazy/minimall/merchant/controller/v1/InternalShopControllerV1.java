package com.yirancrazy.minimall.merchant.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户内部控制器，提供Shop相关内部接口
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
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
        // 内部服务间调用无商家上下文，传 null 跳过归属校验
        ShopPO s = shopService.getById(null, id);
        ShopStatusEnum status = ShopStatusEnum.fromCode(s.getStatus());
        return Result.success(new ShopSnapshotDTO(s.getId(), s.getShopName(),
            status == null ? null : status.getAlias()));
    }
}