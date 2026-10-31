package com.yirancrazy.minimall.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.api.dto.stock.StockInitDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDto;
import com.yirancrazy.minimall.goods.dto.SkuPageDto;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 维护 SKU 主数据和商家归属校验，协调库存服务、父 SPU 搜索索引刷新及快照查询。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class SkuServiceImpl implements SkuService {

  private final SkuManager skuManager;
  private final SpuManager spuManager;
  private final SpuService spuService;
  private final GoodsRemoteGateway goodsRemoteGateway;

  /**
   * 注入SKU 数据访问管理器、SPU 数据访问管理器、SPU 聚合服务、店铺与库存远程调用网关，构造 SkuServiceImpl 实例。
   *
   * @param skuManager SKU 数据访问管理器
   * @param spuManager SPU 数据访问管理器
   * @param spuService SPU 聚合服务
   * @param goodsRemoteGateway 店铺与库存远程调用网关
   */
  public SkuServiceImpl(
      SkuManager skuManager,
      SpuManager spuManager,
      SpuService spuService,
      GoodsRemoteGateway goodsRemoteGateway) {
    this.skuManager = skuManager;
    this.spuManager = spuManager;
    this.spuService = spuService;
    this.goodsRemoteGateway = goodsRemoteGateway;
  }

  /**
   * 按主键读取 SKU，不存在时抛出 SKU_NOT_FOUND。
   *
   * @param id SKU 主键
   * @return 匹配的 SKU 持久化对象
   */
  public SkuPo getById(Long id) {
    SkuPo s = skuManager.getById(id);
    if (s == null) {
      throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
    }
    return s;
  }

  /**
   * 读取 SKU 并校验商家归属；不存在或越权统一抛出 SKU_NOT_FOUND。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 匹配的 SKU 持久化对象
   */
  public SkuPo getById(Long id, Long merchantId) {
    SkuPo s = getById(id);
    checkOwner(s, merchantId);
    return s;
  }

  /**
   * 读取并校验商家归属后将 SKU 转换为视图对象。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 转换后的 SKU 视图对象
   */
  @Override
  public SkuVo getVo(Long id, Long merchantId) {
    return SkuVo.from(getById(id, merchantId));
  }

  /**
   * 校验记录的商家归属；越权访问按记录不存在处理。
   *
   * @param existing 已持久化的商品记录
   * @param merchantId 当前商家 ID，用于商品归属校验
   */
  private void checkOwner(SkuPo existing, Long merchantId) {
    if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
      throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
    }
  }

  /**
   * 校验父 SPU 归属后保存 SKU，尽力初始化库存并刷新父 SPU 搜索文档。
   *
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param dto SKU 创建参数
   * @return 新建记录的主键
   */
  @Override
  public Long create(Long merchantId, SkuCreateDto dto) {
    SpuPo spu = spuManager.getById(dto.getSpuId());
    if (spu == null || spu.getMerchantId() == null || !spu.getMerchantId().equals(merchantId)) {
      throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
    }
    SkuPo sku = new SkuPo();
    sku.setSpuId(dto.getSpuId());
    sku.setMerchantId(merchantId);
    sku.setSkuName(dto.getSkuName());
    sku.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
    sku.setStock(dto.getStock() != null ? dto.getStock() : 0);
    skuManager.save(sku);
    initStockBestEffort(sku, merchantId);
    // 新 SKU 可能改变父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
    spuService.refreshEsDocument(dto.getSpuId());
    return sku.getId();
  }

  /**
   * 尽力调用库存服务初始化 SKU 库存，失败仅记录日志，不阻断商品主链路。
   *
   * @param sku 待初始化或刷新的 SKU 记录
   * @param merchantId 当前商家 ID，用于商品归属校验
   */
  private void initStockBestEffort(SkuPo sku, Long merchantId) {
    try {
      Result<Void> r =
          goodsRemoteGateway.initStock(new StockInitDto(sku.getId(), merchantId, sku.getStock()));
      if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
        log.warn("初始化库存失败，skuId={}，msg={}", sku.getId(), r == null ? "无响应" : r.getMessage());
      }
    } catch (Exception e) {
      log.warn("初始化库存异常，skuId={}", sku.getId(), e);
    }
  }

  /**
   * 按商家和 SKU 名称游标分页查询持久化记录。
   *
   * @param dto SKU 游标分页参数
   * @return 按主键游标分页的结果；记录项为匹配的 SKU 持久化对象
   */
  public CursorPageVo<SkuPo> page(SkuPageDto dto) {
    Long lastId = CursorUtils.decode(dto.getCursor());
    int limit = dto.getLimit();
    List<SkuPo> records =
        skuManager.list(
            Wrappers.lambdaQuery(SkuPo.class)
                .eq(dto.getMerchantId() != null, SkuPo::getMerchantId, dto.getMerchantId())
                .lt(lastId != null, SkuPo::getId, lastId)
                .like(
                    dto.getSkuName() != null && !dto.getSkuName().isBlank(),
                    SkuPo::getSkuName,
                    dto.getSkuName())
                .orderByDesc(SkuPo::getId)
                .last("LIMIT " + (limit + 1)));
    return CursorPageVo.of(records, limit, SkuPo::getId);
  }

  /**
   * 游标分页查询 SKU 并转换为视图对象。
   *
   * @param dto SKU 游标分页参数
   * @return 按主键游标分页的结果；记录项为转换后的 SKU 视图对象
   */
  @Override
  public CursorPageVo<SkuVo> pageVo(SkuPageDto dto) {
    return page(dto).map(SkuVo::from);
  }

  /**
   * 更新 SKU 增量字段；库存变化先调用库存服务，本地失败时反向补偿。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param dto SKU 增量更新参数
   * @return 操作实际影响记录时为 true，否则为 false
   */
  @Override
  public boolean update(Long id, Long merchantId, SkuUpdateDto dto) {
    SkuPo existing = skuManager.getById(id);
    if (existing == null) {
      throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
    }
    checkOwner(existing, merchantId);
    existing.setSkuName(dto.getSkuName());
    if (dto.getPrice() != null) {
      existing.setPrice(dto.getPrice());
    }
    Integer originalStock = existing.getStock();
    Integer stockDelta = null;
    if (dto.getStock() != null && !dto.getStock().equals(existing.getStock())) {
      int currentStock = existing.getStock() == null ? 0 : existing.getStock();
      int delta = dto.getStock() - currentStock;
      stockDelta = delta;
      adjustStock(existing.getId(), merchantId, delta, "商品规格库存调整");
      existing.setStock(dto.getStock());
    }
    boolean ok = skuManager.updateById(existing);
    if (!ok && stockDelta != null) {
      existing.setStock(originalStock);
      rollbackStockAdjustment(existing.getId(), merchantId, -stockDelta);
      throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
    }
    if (ok) {
      // 改价会影响父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
      spuService.refreshEsDocument(existing.getSpuId());
    }
    return ok;
  }

  /**
   * 调用库存服务调整库存；远程无响应或返回失败时抛出业务异常。
   *
   * @param skuId SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param delta 本次库存调整增量
   * @param reason 驳回或库存调整原因
   */
  private void adjustStock(Long skuId, Long merchantId, long delta, String reason) {
    Result<Void> result =
        goodsRemoteGateway.adjustStock(new StockAdjustDto(skuId, merchantId, delta, reason));
    if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
      throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
    }
  }

  /**
   * 尽力反向调整库存；补偿失败仅记录错误日志。
   *
   * @param skuId SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @param delta 本次库存调整增量
   */
  private void rollbackStockAdjustment(Long skuId, Long merchantId, long delta) {
    try {
      adjustStock(skuId, merchantId, delta, "商品规格库存调整回滚");
    } catch (Exception e) {
      log.error("回滚库存调整失败，skuId={}，quantity={}", skuId, delta, e);
    }
  }

  /**
   * 删除 SKU；成功后刷新父 SPU 搜索文档，失败时返回 false。
   *
   * @param id SKU 主键
   * @param merchantId 当前商家 ID，用于商品归属校验
   * @return 操作实际影响记录时为 true，否则为 false
   */
  @Override
  public boolean delete(Long id, Long merchantId) {
    SkuPo existing = skuManager.getById(id);
    if (existing == null) {
      throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
    }
    checkOwner(existing, merchantId);
    Long spuId = existing.getSpuId();
    boolean ok = skuManager.removeById(id);
    if (ok) {
      // 删除 SKU 可能改变父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
      spuService.refreshEsDocument(spuId);
    }
    return ok;
  }

  /**
   * 批量读取 SKU 与父 SPU，仅返回父 SPU 在售的记录并以 SKU 主键建映射。
   *
   * @param skuIds 待查询的 SKU 主键列表，空列表返回空映射
   * @return 以 SKU 主键为键的快照映射；缺失或非在售记录不包含
   */
  @Override
  public Map<Long, SkuSnapshotDto> listSnapshots(List<Long> skuIds) {
    List<Long> ids =
        skuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    List<SkuPo> skus = skuManager.list(Wrappers.lambdaQuery(SkuPo.class).in(SkuPo::getId, ids));
    if (skus.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> spuIds =
        skus.stream()
            .map(SkuPo::getSpuId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    Map<Long, SpuPo> spuMap =
        spuIds.isEmpty()
            ? Collections.emptyMap()
            : spuManager.list(Wrappers.lambdaQuery(SpuPo.class).in(SpuPo::getId, spuIds)).stream()
                .collect(Collectors.toMap(SpuPo::getId, Function.identity()));
    Map<Long, SkuSnapshotDto> snapshots = new HashMap<>(ids.size() * 2);
    for (SkuPo s : skus) {
      SpuPo spu = s.getSpuId() == null ? null : spuMap.get(s.getSpuId());
      // 非在售/孤儿 SPU 的 SKU 不返回，购物车侧展示降级，防止未过审商品被下单
      if (spu == null
          || spu.getStatus() == null
          || spu.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
        continue;
      }
      snapshots.put(
          s.getId(),
          new SkuSnapshotDto(
              s.getId(),
              s.getSpuId(),
              s.getSkuName(),
              s.getPrice(),
              s.getStock(),
              spu.getMerchantId()));
    }
    return snapshots;
  }

  /**
   * 读取 SKU 和父 SPU；父 SPU 缺失或非在售时抛出对应业务异常。
   *
   * @param id SKU 主键
   * @return SKU 价格、库存和商家归属快照
   */
  @Override
  public SkuSnapshotDto getSnapshot(Long id) {
    SkuPo sku = getById(id);
    SpuPo spu = spuManager.getById(sku.getSpuId());
    if (spu == null) {
      throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
    }
    if (spu.getStatus() == null || spu.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
      throw new BizException(SpuCodeEnum.SPU_NOT_ON_SALE);
    }
    return new SkuSnapshotDto(
        sku.getId(),
        sku.getSpuId(),
        sku.getSkuName(),
        sku.getPrice(),
        sku.getStock(),
        spu.getMerchantId());
  }
}
