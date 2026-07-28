package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/goods/sku")
public class InternalSkuControllerV1 {

    private final SkuService skuService;

    public InternalSkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    @GetMapping("/{id}")
    public Result<SkuSnapshotDTO> snapshot(@PathVariable Long id) {
        SkuPO s = skuService.getById(id);
        return Result.success(new SkuSnapshotDTO(s.getId(), s.getSpuId(), s.getSkuName(),
            s.getPrice(), s.getStock()));
    }
}