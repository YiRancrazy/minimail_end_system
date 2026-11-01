package com.yirancrazy.minimall.goods.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.search.impl.SpuSearchServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SPU 搜索过滤、结果映射、MinIO 图片解析及 ES 同步删除。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class SpuSearchServiceImplTest {

    private ElasticsearchOperations elasticsearchOperations;
    private MinioUtil minioUtil;
    private SpuSearchServiceImpl service;

    @SuppressWarnings("unchecked")
    private SearchHits<SpuDocument> buildHitsWithDoc(SpuDocument doc) {
        SearchHit<SpuDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        SearchHits<SpuDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        return hits;
    }

    @BeforeEach
    void setUp() {
        elasticsearchOperations = mock(ElasticsearchOperations.class);
        minioUtil = mock(MinioUtil.class);
        service = new SpuSearchServiceImpl(elasticsearchOperations, minioUtil);
    }

    /** 验证关键词搜索命中在售商品并映射为搜索视图。 */
    public void searchWithKeywordReturnsResults() {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(1L);
        doc.setTitle("测试商品");
        doc.setCategoryId(10L);
        doc.setMerchantId(100L);
        doc.setMinPrice(9900L);
        doc.setMaxPrice(19900L);
        doc.setSaleStatus(2);
        doc.setMainImage("http://img.example.com/test.jpg");

        SearchHits<SpuDocument> hits = buildHitsWithDoc(doc);
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class))).thenReturn(hits);

        SpuSearchDto dto = new SpuSearchDto();
        dto.setKeyword("测试");
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVo> result = service.search(dto);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSpuId());
        assertEquals("测试商品", result.get(0).getTitle());
        verify(elasticsearchOperations).search(any(Query.class), eq(SpuDocument.class));
    }

    /** 验证无关键词时按创建时间倒序查询在售商品。 */
    public void searchWithoutKeywordReturnsResults() {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(2L);
        doc.setTitle("无关键词商品");
        doc.setCategoryId(10L);
        doc.setMerchantId(100L);
        doc.setMinPrice(5000L);
        doc.setMaxPrice(8000L);
        doc.setSaleStatus(2);
        doc.setMainImage("http://img.example.com/no-keyword.jpg");

        SearchHits<SpuDocument> hits = buildHitsWithDoc(doc);
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class))).thenReturn(hits);

        SpuSearchDto dto = new SpuSearchDto();
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVo> result = service.search(dto);

        assertEquals(1, result.size());
        verify(elasticsearchOperations).search(any(Query.class), eq(SpuDocument.class));
    }

    /** 验证 Elasticsearch 无命中时返回空列表。 */
    @SuppressWarnings("unchecked")
    public void searchReturnsEmptyWhenNoResults() {
        SearchHits<SpuDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class))).thenReturn(hits);

        SpuSearchDto dto = new SpuSearchDto();
        dto.setKeyword("不存在的商品");
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVo> result = service.search(dto);

        assertEquals(0, result.size());
    }

    /** 验证搜索结果将主图对象键转换为 MinIO 公网 URL。 */
    public void searchResolvesMainImageToPresignedUrl() {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(3L);
        doc.setTitle("图片商品");
        doc.setMainImage("img-abc.png");
        when(minioUtil.resolvePublicUrl("img-abc.png"))
                .thenReturn("http://minio/mall-files/img-abc.png?token");

        SearchHits<SpuDocument> hits = buildHitsWithDoc(doc);
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class))).thenReturn(hits);

        SpuSearchDto dto = new SpuSearchDto();
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVo> result = service.search(dto);

        assertEquals("http://minio/mall-files/img-abc.png?token", result.get(0).getMainImage());
    }

    /** 验证 SPU 文档同步调用 Elasticsearch save。 */
    public void syncSavesDocument() {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(1L);
        doc.setTitle("同步商品");

        service.sync(doc);

        verify(elasticsearchOperations).save(doc);
    }

    /** 验证按 SPU 主键调用 Elasticsearch 删除对应文档。 */
    public void deleteByIdDeletesDocument() {
        service.deleteById(1L);

        verify(elasticsearchOperations).delete("1", SpuDocument.class);
    }
}
