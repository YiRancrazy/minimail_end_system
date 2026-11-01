package com.yirancrazy.minimall.goods.search;

import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SPU 搜索查询、Elasticsearch 索引同步和文档删除能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface SpuSearchService {

    /**
     * 按关键词、类目和价格区间搜索在售 SPU，结果按相关性或创建时间排序。
     *
     * @param dto 关键词、类目、价格和分页搜索参数
     * @return 当前页搜索命中的商品视图列表；无命中时返回空列表
     */
    List<SpuSearchVo> search(SpuSearchDto dto);

    /**
     * 写入或覆盖 SPU 的 Elasticsearch 搜索文档。
     *
     * @param document 待写入 Elasticsearch 的 SPU 文档
     */
    void sync(SpuDocument document);

    /**
     * 按 SPU 主键删除 Elasticsearch 搜索文档。
     *
     * @param id SPU 主键
     */
    void deleteById(Long id);
}
