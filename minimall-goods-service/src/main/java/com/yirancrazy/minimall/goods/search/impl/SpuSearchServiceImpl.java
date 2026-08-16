package com.yirancrazy.minimall.goods.search.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品搜索服务实现，基于 ElasticsearchOperations 实现全文检索与过滤
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@Service
public class SpuSearchServiceImpl implements SpuSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SpuSearchServiceImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /**
     * ES 商品搜索，支持关键词全文检索 + 分类/价格范围过滤，仅返回在售商品。
     * 有关键词时按相关性排序，无关键词时按 createTime 降序。
     * @param dto 搜索入参
     * @return 搜索结果列表
     */
    @Override
    public List<SpuSearchVO> search(SpuSearchDTO dto) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 固定过滤：仅返回在售商品
        boolBuilder.filter(fq -> fq
            .term(tq -> tq.field("saleStatus").value(SpuStatusEnum.ON_SALE.statusValue())));

        // 关键词全文检索
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            boolBuilder.must(mq -> mq
                .match(mmq -> mmq.field("title").query(dto.getKeyword())));
        }

        // 分类过滤
        if (dto.getCategoryId() != null) {
            boolBuilder.filter(fq -> fq
                .term(tq -> tq.field("categoryId").value(dto.getCategoryId())));
        }

        // 价格范围过滤
        if (dto.getMinPrice() != null || dto.getMaxPrice() != null) {
            boolBuilder.filter(fq -> fq
                .range(rq -> configurePriceRange(rq, dto.getMinPrice(), dto.getMaxPrice())));
        }

        // 排序：有关键词时按相关性，无关键词时按 createTime 降序
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
            .withQuery(q -> q.bool(boolBuilder.build()))
            .withPageable(PageRequest.of(
                dto.getPageNo() - 1, dto.getPageSize()));

        if (dto.getKeyword() == null || dto.getKeyword().isBlank()) {
            queryBuilder.withSort(
                Sort.by(Sort.Order.desc("createTime")));
        }

        SearchHits<SpuDocument> hits = elasticsearchOperations.search(
            queryBuilder.build(), SpuDocument.class);

        return hits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    /**
     * 同步 SPU 文档到 ES 索引。
     * @param document SPU 文档
     */
    @Override
    public void sync(SpuDocument document) {
        elasticsearchOperations.save(document);
        log.info("spu synced to ES, spuId={}", document.getSpuId());
    }

    /**
     * 按 SPU 主键删除 ES 索引中的文档。
     * @param id SPU 主键 ID
     */
    @Override
    public void deleteById(Long id) {
        elasticsearchOperations.delete(String.valueOf(id), SpuDocument.class);
        log.info("spu deleted from ES, spuId={}", id);
    }

    private RangeQuery.Builder configurePriceRange(RangeQuery.Builder rq, Long minPrice, Long maxPrice) {
        rq.field("minPrice");
        if (minPrice != null) {
            rq.gte(JsonData.of(minPrice));
        }
        if (maxPrice != null) {
            rq.lte(JsonData.of(maxPrice));
        }
        return rq;
    }

    private SpuSearchVO toVO(SpuDocument doc) {
        return new SpuSearchVO(
            doc.getSpuId(),
            doc.getTitle(),
            doc.getMinPrice(),
            doc.getMaxPrice(),
            doc.getMainImage(),
            doc.getMerchantId()
        );
    }
}
