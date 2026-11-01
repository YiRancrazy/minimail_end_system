package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.service.SkuService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 面向内部服务提供在售 SKU 快照查询，供购物车和订单链路读取商品价格与库存。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/goods/sku")
public class InternalSkuControllerV1 {

    private final SkuService skuService;

    /**
     * 注入SKU 业务服务，构造 InternalSkuControllerV1 实例。
     *
     * @param skuService SKU 业务服务
     */
    public InternalSkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }

    /**
     * 读取内部 SKU 快照，供购物车或订单链路获取价格、库存和商家。
     *
     * @param id SKU 主键
     * @return 统一响应包装；data 为SKU 价格、库存和商家归属快照
     */
    @GetMapping("/{id}")
    public Result<SkuSnapshotDto> snapshot(@PathVariable Long id) {
        return Result.success(skuService.getSnapshot(id));
    }

    /**
     * 批量读取 SKU 快照，忽略空主键并以 SKU 主键建立返回映射。
     *
     * @param skuIds 待查询的 SKU 主键列表，空列表返回空映射
     * @return 统一响应包装；data 为以 SKU 主键为键的快照映射；缺失或非在售记录不包含
     */
    @PostMapping("/batch-snapshot")
    public Result<Map<Long, SkuSnapshotDto>> batchSnapshot(@RequestBody List<Long> skuIds) {
        return Result.success(skuService.listSnapshots(skuIds));
    }
}
