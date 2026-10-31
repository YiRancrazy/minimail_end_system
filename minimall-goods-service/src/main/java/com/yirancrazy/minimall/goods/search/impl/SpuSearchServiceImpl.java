package com.yirancrazy.minimall.goods.search.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 ElasticsearchOperations 提供在售 SPU 搜索、索引同步和删除，并将主图对象键转换为可访问 URL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class SpuSearchServiceImpl implements SpuSearchService {

  private final ElasticsearchOperations elasticsearchOperations;
  private final MinioUtil minioUtil;

  /**
   * 注入Elasticsearch 查询与索引操作入口、MinIO 对象键到公网 URL 的解析工具，构造 SpuSearchServiceImpl 实例。
   *
   * @param elasticsearchOperations Elasticsearch 查询与索引操作入口
   * @param minioUtil MinIO 对象键到公网 URL 的解析工具
   */
  public SpuSearchServiceImpl(
      ElasticsearchOperations elasticsearchOperations, MinioUtil minioUtil) {
    this.elasticsearchOperations = elasticsearchOperations;
    this.minioUtil = minioUtil;
  }

  /**
   * 构造 Elasticsearch 布尔查询，固定过滤在售商品，支持标题关键词、类目和价格区间，并将命中映射为搜索视图。
   *
   * @param dto 关键词、类目、价格和分页搜索参数
   * @return 当前页搜索命中的商品视图列表；无命中时返回空列表
   */
  @Override
  public List<SpuSearchVo> search(SpuSearchDto dto) {
    BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

    // 固定过滤：仅返回在售商品
    boolBuilder.filter(
        fq -> fq.term(tq -> tq.field("saleStatus").value(SpuStatusEnum.ON_SALE.statusValue())));

    // 关键词全文检索
    if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
      boolBuilder.must(mq -> mq.match(mmq -> mmq.field("title").query(dto.getKeyword())));
    }

    // 分类过滤
    if (dto.getCategoryId() != null) {
      boolBuilder.filter(fq -> fq.term(tq -> tq.field("categoryId").value(dto.getCategoryId())));
    }

    // 价格范围过滤
    if (dto.getMinPrice() != null || dto.getMaxPrice() != null) {
      boolBuilder.filter(
          fq -> fq.range(rq -> configurePriceRange(rq, dto.getMinPrice(), dto.getMaxPrice())));
    }

    // 排序：有关键词时按相关性，无关键词时按 createTime 降序
    NativeQueryBuilder queryBuilder =
        NativeQuery.builder()
            .withQuery(q -> q.bool(boolBuilder.build()))
            .withPageable(PageRequest.of(dto.getPageNo() - 1, dto.getPageSize()));

    if (dto.getKeyword() == null || dto.getKeyword().isBlank()) {
      queryBuilder.withSort(Sort.by(Sort.Order.desc("createTime")));
    }

    SearchHits<SpuDocument> hits =
        elasticsearchOperations.search(queryBuilder.build(), SpuDocument.class);

    return hits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .map(this::toVo)
        .collect(Collectors.toList());
  }

  /**
   * 将完整 SPU 文档写入 Elasticsearch，覆盖同主键旧文档。
   *
   * @param document 待写入 Elasticsearch 的 SPU 文档
   */
  public void sync(SpuDocument document) {
    elasticsearchOperations.save(document);
    log.info("spu 已同步到 ES，spuId={}", document.getSpuId());
  }

  /**
   * 按 SPU 主键删除 Elasticsearch 文档。
   *
   * @param id SPU 主键
   */
  public void deleteById(Long id) {
    elasticsearchOperations.delete(String.valueOf(id), SpuDocument.class);
    log.info("spu 已从 ES 删除，spuId={}", id);
  }

  /**
   * 为价格过滤补充 minPrice 字段的 gte/lte 条件，空端点按开区间处理。
   *
   * @param rq 待补充价格区间的 Elasticsearch RangeQuery Builder
   * @param minPrice 最低价格过滤值，单位为分
   * @param maxPrice 最高价格过滤值，单位为分
   * @return 补充完成的价格范围查询构造器
   */
  private RangeQuery.Builder configurePriceRange(
      RangeQuery.Builder rq, Long minPrice, Long maxPrice) {
    rq.field("minPrice");
    if (minPrice != null) {
      rq.gte(JsonData.of(minPrice));
    }
    if (maxPrice != null) {
      rq.lte(JsonData.of(maxPrice));
    }
    return rq;
  }

  /**
   * 将搜索文档映射为视图，并将主图对象键转换为 MinIO 公网 URL。
   *
   * @param doc Elasticsearch 返回的 SPU 文档
   * @return 搜索视图对象
   */
  private SpuSearchVo toVo(SpuDocument doc) {
    return new SpuSearchVo(
        doc.getSpuId(),
        doc.getTitle(),
        doc.getMinPrice(),
        doc.getMaxPrice(),
        minioUtil.resolvePublicUrl(doc.getMainImage()),
        doc.getMerchantId());
  }
}
