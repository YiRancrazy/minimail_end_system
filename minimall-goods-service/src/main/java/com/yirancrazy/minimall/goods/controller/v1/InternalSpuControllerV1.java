package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.service.SpuService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalSpuControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/internal/goods/spu")
public class InternalSpuControllerV1 {

    private final SpuService spuService;

    /**
     * 构造 InternalSpuControllerV1 实例。
     * @param spuService spuService 参数
     */
    public InternalSpuControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }


    /**
     * 执行 snapshot 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<SpuSnapshotDTO> snapshot(@PathVariable Long id) {
        return Result.success(spuService.getSnapshot(id));
    }


    /**
     * 执行 batchSnapshot 方法。
     * @param spuIds spuIds 参数
     * @return 统一响应结果
     */
    @PostMapping("/batch-snapshot")
    public Result<Map<Long, SpuSnapshotDTO>> batchSnapshot(@RequestBody List<Long> spuIds) {
        return Result.success(spuService.listSnapshots(spuIds));
    }
}
