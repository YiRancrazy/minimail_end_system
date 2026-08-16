package com.yirancrazy.minimall.goods.search;

import java.util.List;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品搜索服务接口，定义 ES 搜索与数据同步契约
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public interface SpuSearchService {

    /**
     * ES 商品搜索，支持关键词全文检索 + 分类/价格范围过滤，仅返回在售商品。
     * @param dto 搜索入参
     * @return 搜索结果列表
     */
    List<SpuSearchVO> search(SpuSearchDTO dto);

    /**
     * 同步 SPU 文档到 ES 索引。
     * @param document SPU 文档
     */
    void sync(SpuDocument document);

    /**
     * 按 SPU 主键删除 ES 索引中的文档，与 SPU 删除保持镜像一致。
     * @param id SPU 主键 ID
     */
    void deleteById(Long id);
}
