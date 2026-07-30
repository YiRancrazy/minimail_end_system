package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* 商品内部接口控制器，供其他微服务（如订单、库存）按 ID 查询 SKU 快照数据。
 */
@RestController
@RequestMapping("/internal/goods/sku")
public class InternalSkuControllerV1 {

    private final SkuService skuService;

    public InternalSkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    /**
     * 查询 SKU 快照信息，供其他服务在跨链路调用时获取精简字段。
     *
     * @param id SKU 主键 ID
     * @return SKU 快照 DTO，含名称、价格、库存等核心展示字段
     */
    @GetMapping("/{id}")
    public Result<SkuSnapshotDTO> snapshot(@PathVariable Long id) {
        SkuPO s = skuService.getById(id);
        return Result.success(new SkuSnapshotDTO(s.getId(), s.getSpuId(), s.getSkuName(),
            s.getPrice(), s.getStock()));
    }
}