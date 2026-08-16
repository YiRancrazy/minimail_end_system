package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品内部控制器，提供Sku相关内部接口
 * @Version: 1.2
 * @DateTime: 2026/08/05
 */
@RestController
@RequestMapping("/internal/goods/sku")
public class InternalSkuControllerV1 {

    private final SkuService skuService;
    private final SpuService spuService;

    public InternalSkuControllerV1(SkuService skuService, SpuService spuService) {
        this.skuService = skuService;
        this.spuService = spuService;
    }

    /**
     * 查询 SKU 快照信息，供其他服务在跨链路调用时获取精简字段，含归属商家 ID。
     * 仅当所属 SPU 处于在售状态时返回，防止未过审/下架商品绕过用户端校验进入下单链路。
     * @param id SKU 主键 ID
     * @return SKU 快照 DTO，含名称、价格、库存、商家ID等核心字段
     * @throws BizException 当 SKU/SPU 不存在或所属 SPU 非在售状态时
     */
    @GetMapping("/{id}")
    public Result<SkuSnapshotDTO> snapshot(@PathVariable Long id) {
        SkuPO s = skuService.getById(id);
        SpuPO spu = spuService.getById(s.getSpuId());
        if (spu.getStatus() == null || spu.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_ON_SALE);
        }
        return Result.success(new SkuSnapshotDTO(s.getId(), s.getSpuId(), s.getSkuName(),
            s.getPrice(), s.getStock(), spu.getMerchantId()));
    }

    /**
     * 批量查询 SKU 快照，供购物车列表等跨服务链路一次调用替代逐 SKU 的 N 次请求；
     * 返回契约与单条快照一致（仅在售 SPU 的 SKU 入 Map），缺失项由调用方兜底降级。
     * @param skuIds SKU 主键集合，允许为空
     * @return skuId -> SKU 快照 Map
     */
    @PostMapping("/batch-snapshot")
    public Result<Map<Long, SkuSnapshotDTO>> batchSnapshot(@RequestBody List<Long> skuIds) {
        return Result.success(skuService.listSnapshots(skuIds));
    }
}
