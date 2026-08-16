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
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品内部控制器，提供Spu相关内部接口，供购物车等跨服务链路获取商品标题与主图。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 */
@RestController
@RequestMapping("/internal/goods/spu")
public class InternalSpuControllerV1 {

    private final SpuService spuService;

    public InternalSpuControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }

    /**
     * 查询 SPU 快照信息，供其他服务在跨链路调用时获取商品标题与主图等展示字段。
     * @param id SPU 主键 ID
     * @return SPU 快照 DTO；不存在时返回空快照
     */
    @GetMapping("/{id}")
    public Result<SpuSnapshotDTO> snapshot(@PathVariable Long id) {
        SpuPO spu = spuService.getById(id);
        if (spu == null) {
            return Result.success(new SpuSnapshotDTO(id, null, null));
        }
        return Result.success(new SpuSnapshotDTO(spu.getId(), spu.getTitle(), spu.getMainImageUrl()));
    }

    /**
     * 批量查询 SPU 快照，供购物车列表等跨服务链路一次调用替代逐 SPU 的 N 次请求；
     * 返回契约与单条快照一致，不存在的 SPU 不放入 Map，由调用方兜底降级。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> SPU 快照 Map
     */
    @PostMapping("/batch-snapshot")
    public Result<Map<Long, SpuSnapshotDTO>> batchSnapshot(@RequestBody List<Long> spuIds) {
        return Result.success(spuService.listSnapshots(spuIds));
    }
}
