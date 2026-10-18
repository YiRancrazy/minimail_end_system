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
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.service.SkuService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalSkuControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/goods/sku")
public class InternalSkuControllerV1 {

    private final SkuService skuService;

    /**
     * 构造 InternalSkuControllerV1 实例。
     * @param skuService skuService 参数
     */
    public InternalSkuControllerV1(SkuService skuService) {
        this.skuService = skuService;
    }


    /**
     * 执行 snapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<SkuSnapshotDTO> snapshot(@PathVariable Long id) {
        return Result.success(skuService.getSnapshot(id));
    }


    /**
     * 执行 batchSnapshot 方法。
     * @param skuIds skuIds 参数
     * @return 统一响应结果
     */
    @PostMapping("/batch-snapshot")
    public Result<Map<Long, SkuSnapshotDTO>> batchSnapshot(@RequestBody List<Long> skuIds) {
        return Result.success(skuService.listSnapshots(skuIds));
    }
}
