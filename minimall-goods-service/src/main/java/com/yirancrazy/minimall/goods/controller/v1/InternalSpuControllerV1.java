package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.service.SpuService;
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
 * @Description: 面向内部服务提供 SPU 快照查询，供订单链路读取商品标题和主图。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/goods/spu")
public class InternalSpuControllerV1 {

  private final SpuService spuService;

  /**
   * 注入SPU 聚合服务，构造 InternalSpuControllerV1 实例。
   *
   * @param spuService SPU 聚合服务
   */
  public InternalSpuControllerV1(SpuService spuService) {
    this.spuService = spuService;
  }

  /**
   * 读取内部 SPU 快照；记录不存在时返回标题和主图为空的对象。
   *
   * @param id SPU 主键
   * @return 统一响应包装；data 为SPU 标题和主图快照
   */
  @GetMapping("/{id}")
  public Result<SpuSnapshotDto> snapshot(@PathVariable Long id) {
    return Result.success(spuService.getSnapshot(id));
  }

  /**
   * 批量读取 SPU 快照，并以 SPU 主键建立返回映射。
   *
   * @param spuIds 待查询的 SPU 主键列表，空列表返回空映射
   * @return 统一响应包装；data 为以 SPU 主键为键的快照映射；不存在的记录不包含
   */
  @PostMapping("/batch-snapshot")
  public Result<Map<Long, SpuSnapshotDto>> batchSnapshot(@RequestBody List<Long> spuIds) {
    return Result.success(spuService.listSnapshots(spuIds));
  }
}
