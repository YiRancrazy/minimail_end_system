package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.goods.dto.GoodsPageDto;
import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import com.yirancrazy.minimall.goods.vo.SpuDetailVo;
import com.yirancrazy.minimall.goods.vo.SpuListVo;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义用户端在售商品分页、详情、SKU 列表和全文搜索能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface GoodsQueryService {

    /**
     * 分页查询在售商品，并聚合每个 SPU 的 SKU 最低价。
     *
     * @param dto 商品游标分页参数
     * @return 包含商品摘要、最低价和图片 URL 的游标分页结果
     * @throws BizException 查询条件非法或游标无法解析时抛出
     */
    CursorPageVo<SpuListVo> pageOnSale(GoodsPageDto dto);

    /**
     * 查询在售商品详情并组装 SKU 列表。
     *
     * @param spuId SPU 主键
     * @return 在售商品详情及 SKU 列表
     * @throws BizException SPU 不存在或非在售时抛出 SPU_NOT_FOUND
     */
    SpuDetailVo getDetail(Long spuId);

    /**
     * 查询指定在售 SPU 的 SKU 列表。
     *
     * @param spuId SPU 主键
     * @return SKU 视图列表；无 SKU 时返回空列表
     * @throws BizException SPU 不存在或非在售时抛出 SPU_NOT_FOUND
     */
    List<SkuVo> listSkus(Long spuId);

    /**
     * 按搜索条件查询 Elasticsearch 中的在售商品。
     *
     * @param dto 关键词、类目、价格和分页搜索参数
     * @return 搜索命中的商品视图列表；无命中时返回空列表
     */
    List<SpuSearchVo> search(SpuSearchDto dto);
}
