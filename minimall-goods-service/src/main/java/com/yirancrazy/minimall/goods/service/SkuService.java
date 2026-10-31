package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.goods.dto.SkuCreateDto;
import com.yirancrazy.minimall.goods.dto.SkuPageDto;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import java.util.List;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SKU 维护、商家归属校验、快照查询和库存同步所需的业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface SkuService {

  /**
   * 读取 SKU 并校验商家归属；不存在或越权统一抛出 SKU_NOT_FOUND。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 匹配的 SKU 持久化对象
   */
  SkuPo getById(Long id, Long merchantId);

  /**
   * 按主键读取 SKU，不存在时抛出 SKU_NOT_FOUND。
   *
   * @param id SKU 主键
   * @return 匹配的 SKU 持久化对象
   */
  SkuPo getById(Long id);

  /**
   * 读取并校验商家归属后将 SKU 转换为视图对象。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 转换后的 SKU 视图对象
   */
  SkuVo getVo(Long id, Long merchantId);

  /**
   * 在指定父 SPU 下创建 SKU，校验商家归属并初始化库存与搜索镜像。
   *
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param dto SKU 创建参数
   * @return 新建记录的主键
   */
  Long create(Long merchantId, SkuCreateDto dto);

  /**
   * 按商家和 SKU 名称游标分页查询持久化记录。
   *
   * @param dto SKU 游标分页参数
   * @return 按主键游标分页的结果；记录项为匹配的 SKU 持久化对象
   */
  CursorPageVo<SkuPo> page(SkuPageDto dto);

  /**
   * 游标分页查询 SKU 并转换为视图对象。
   *
   * @param dto SKU 游标分页参数
   * @return 按主键游标分页的结果；记录项为转换后的 SKU 视图对象
   */
  CursorPageVo<SkuVo> pageVo(SkuPageDto dto);

  /**
   * 更新 SKU 名称、价格和库存，并执行商家归属校验。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param dto SKU 增量更新参数
   * @return 操作实际影响记录时为 true，否则为 false
   */
  boolean update(Long id, Long merchantId, SkuUpdateDto dto);

  /**
   * 删除 SKU 并执行商家归属校验。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 操作实际影响记录时为 true，否则为 false
   */
  boolean delete(Long id, Long merchantId);

  /**
   * 批量读取 SKU 快照，仅保留父 SPU 在售的记录。
   *
   * @param skuIds 待查询的 SKU 主键列表，空列表返回空映射
   * @return 以 SKU 主键为键的快照映射；缺失或非在售记录不包含
   */
  Map<Long, SkuSnapshotDto> listSnapshots(List<Long> skuIds);

  /**
   * 读取单个 SKU 快照，父 SPU 必须存在且在售。
   *
   * @param id SKU 主键
   * @return SKU 价格、库存和商家归属快照
   */
  SkuSnapshotDto getSnapshot(Long id);
}
