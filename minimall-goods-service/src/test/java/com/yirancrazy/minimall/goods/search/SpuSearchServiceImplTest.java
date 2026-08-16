package com.yirancrazy.minimall.goods.search;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.search.impl.SpuSearchServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;

/**
 * SpuSearchServiceImpl 单元测试，覆盖 ES 搜索与同步的正常、边界路径。
 */
public class SpuSearchServiceImplTest {

    private ElasticsearchOperations elasticsearchOperations;
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
        service = new SpuSearchServiceImpl(elasticsearchOperations);
    }

    /**
     * 验证 search 有关键词时委托给 elasticsearchOperations.search 并返回结果。
     */
    @Test
    public void search_with_keyword_returns_results() {
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
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class)))
            .thenReturn(hits);

        SpuSearchDTO dto = new SpuSearchDTO();
        dto.setKeyword("测试");
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVO> result = service.search(dto);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getSpuId());
        assertEquals("测试商品", result.get(0).getTitle());
        verify(elasticsearchOperations).search(any(Query.class), eq(SpuDocument.class));
    }

    /**
     * 验证 search 无关键词时仍能返回结果（过滤查询）。
     */
    @Test
    public void search_without_keyword_returns_results() {
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
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class)))
            .thenReturn(hits);

        SpuSearchDTO dto = new SpuSearchDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVO> result = service.search(dto);

        assertEquals(1, result.size());
        verify(elasticsearchOperations).search(any(Query.class), eq(SpuDocument.class));
    }

    /**
     * 验证 search 无结果时返回空列表。
     */
    @Test
    @SuppressWarnings("unchecked")
    public void search_returns_empty_when_no_results() {
        SearchHits<SpuDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(elasticsearchOperations.search(any(Query.class), eq(SpuDocument.class)))
            .thenReturn(hits);

        SpuSearchDTO dto = new SpuSearchDTO();
        dto.setKeyword("不存在的商品");
        dto.setPageNo(1);
        dto.setPageSize(10);

        List<SpuSearchVO> result = service.search(dto);

        assertEquals(0, result.size());
    }

    /**
     * 验证 sync 委托给 elasticsearchOperations.save。
     */
    @Test
    public void sync_saves_document() {
        SpuDocument doc = new SpuDocument();
        doc.setSpuId(1L);
        doc.setTitle("同步商品");

        service.sync(doc);

        verify(elasticsearchOperations).save(doc);
    }

    /**
     * 验证 deleteById 委托给 elasticsearchOperations.delete，id 转为字符串后删除指定类型文档。
     */
    @Test
    public void deleteById_deletes_document() {
        service.deleteById(1L);

        verify(elasticsearchOperations).delete("1", SpuDocument.class);
    }
}
