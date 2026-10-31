package com.yirancrazy.minimall.goods.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.GoodsPageDto;
import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import com.yirancrazy.minimall.goods.vo.SpuDetailVo;
import com.yirancrazy.minimall.goods.vo.SpuListVo;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 聚合 SPU、SKU 管理器和 Elasticsearch，提供在售商品展示查询并解析 MinIO 图片 URL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class GoodsQueryServiceImpl implements GoodsQueryService {

  private final SpuManager spuManager;
  private final SkuManager skuManager;
  private final SpuSearchService spuSearchService;
  private final MinioUtil minioUtil;

  /**
   * 注入SPU 数据访问管理器、SKU 数据访问管理器、Elasticsearch SPU 索引服务、MinIO 对象键到公网 URL 的解析工具，构造
   *   GoodsQueryServiceImpl 实例。
   *
   * @param spuManager SPU 数据访问管理器
   * @param skuManager SKU 数据访问管理器
   * @param spuSearchService Elasticsearch SPU 索引服务
   * @param minioUtil MinIO 对象键到公网 URL 的解析工具
   */
  public GoodsQueryServiceImpl(
      SpuManager spuManager,
      SkuManager skuManager,
      SpuSearchService spuSearchService,
      MinioUtil minioUtil) {
    this.spuManager = spuManager;
    this.skuManager = skuManager;
    this.spuSearchService = spuSearchService;
    this.minioUtil = minioUtil;
  }

  /**
   * 将搜索请求委托给 Elasticsearch 搜索服务，不访问数据库回退。
   *
   * @param dto 关键词、类目、价格和分页搜索参数
   * @return 搜索命中的商品视图列表；无命中时返回空列表
   */
  @Override
  public List<SpuSearchVo> search(SpuSearchDto dto) {
    return spuSearchService.search(dto);
  }

  /**
   * 将 MinIO 对象键解析为客户端可访问的图片 URL。
   *
   * @param objectKey MinIO 对象键；空值按 MinIO 工具规则处理
   * @return 解析后的图片 URL；null 按 MinioUtil 规则处理
   */
  private String resolveImageUrl(String objectKey) {
    return minioUtil.resolvePublicUrl(objectKey);
  }

  /**
   * 按主键倒序分页查询在售 SPU，支持类目和标题过滤，并批量聚合每个 SPU 的 SKU 最低价。
   *
   * @param dto 商品游标分页参数
   * @return 包含在售商品摘要、最低价和 MinIO 图片 URL 的游标分页结果
   */
  @Override
  public CursorPageVo<SpuListVo> pageOnSale(GoodsPageDto dto) {
    Long lastId = CursorUtils.decode(dto.getCursor()); // 上一页最后一条记录的id
    int limit = dto.getLimit(); // 每页数量
    List<SpuPo> records =
        spuManager.list(
            Wrappers.lambdaQuery(SpuPo.class)
                .lt(lastId != null, SpuPo::getId, lastId)
                .eq(SpuPo::getStatus, SpuStatusEnum.ON_SALE.statusValue())
                .eq(dto.getCategoryId() != null, SpuPo::getCategoryId, dto.getCategoryId())
                .like(
                    dto.getKeyword() != null && !dto.getKeyword().isBlank(),
                    SpuPo::getTitle,
                    dto.getKeyword())
                .orderByDesc(SpuPo::getId)
                .last("LIMIT " + (limit + 1)));
    Map<Long, BigDecimal> minPriceMap =
        batchMinPrice(records.stream().map(SpuPo::getId).collect(Collectors.toList()));
    return CursorPageVo.of(records, limit, SpuPo::getId)
        .map(
            po ->
                new SpuListVo(
                    po.getId(),
                    po.getSpuNo(),
                    po.getTitle(),
                    po.getSubtitle(),
                    resolveImageUrl(po.getMainImageUrl()),
                    po.getMerchantId(),
                    minPriceMap.get(po.getId())));
  }

  /**
   * 批量读取多个 SPU 的 SKU，并按 SPU 聚合有效最低价格。
   *
   * @param spuIds 待查询的 SPU 主键列表，空列表返回空映射
   * @return 以 SPU 主键为键的最低价映射；空输入或无有效价格时返回空映射
   */
  private Map<Long, BigDecimal> batchMinPrice(List<Long> spuIds) {
    if (spuIds == null || spuIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<SkuPo> skus =
        skuManager.list(Wrappers.lambdaQuery(SkuPo.class).in(SkuPo::getSpuId, spuIds));
    // 空价 SKU 不参与比价，避免 naturalOrder 对 null 抛 NPE（价格列理论非空，防御脏数据）
    return skus.stream()
        .filter(sku -> sku.getPrice() != null)
        .collect(
            Collectors.groupingBy(
                SkuPo::getSpuId,
                Collectors.mapping(SkuPo::getPrice, Collectors.minBy(Comparator.naturalOrder()))))
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(null)));
  }

  /**
   * 读取在售 SPU 详情并组装全部 SKU；不存在或非在售统一按未找到处理。
   *
   * @param spuId SPU 主键
   * @return 在售商品详情及 SKU 列表
   * @throws BizException SPU 不存在或非在售时抛出 SPU_NOT_FOUND
   */
  @Override
  public SpuDetailVo getDetail(Long spuId) {
    SpuPo po = spuManager.getById(spuId);
    if (po == null
        || po.getStatus() == null
        || po.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
      throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
    }
    List<SkuVo> skus = listSkusInternal(spuId);
    log.info("商品详情查询完成，spuId={}，skuCount={}", spuId, skus.size());
    return new SpuDetailVo(
        po.getId(),
        po.getSpuNo(),
        po.getTitle(),
        po.getSubtitle(),
        resolveImageUrl(po.getMainImageUrl()),
        po.getMerchantId(),
        po.getCategoryId(),
        Boolean.TRUE,
        skus);
  }

  /**
   * 校验 SPU 存在且处于在售状态后返回其全部 SKU。
   *
   * @param spuId SPU 主键
   * @return 在售 SPU 的 SKU 视图列表；无 SKU 时返回空列表
   * @throws BizException SPU 不存在或非在售时抛出 SPU_NOT_FOUND
   */
  @Override
  public List<SkuVo> listSkus(Long spuId) {
    SpuPo po = spuManager.getById(spuId);
    if (po == null
        || po.getStatus() == null
        || po.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
      throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
    }
    return listSkusInternal(spuId);
  }

  /**
   * 直接查询指定 SPU 的 SKU 并转换为视图对象，不做 SPU 状态校验。
   *
   * @param spuId SPU 主键
   * @return SPU 下的 SKU 视图列表；无数据时返回空列表
   */
  private List<SkuVo> listSkusInternal(Long spuId) {
    List<SkuPo> skuPos =
        skuManager.list(Wrappers.lambdaQuery(SkuPo.class).eq(SkuPo::getSpuId, spuId));
    return skuPos.stream().map(SkuVo::from).collect(Collectors.toList());
  }
}
