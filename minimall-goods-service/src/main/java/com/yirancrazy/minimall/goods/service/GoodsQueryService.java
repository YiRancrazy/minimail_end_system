package com.yirancrazy.minimall.goods.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品只读查询服务，仅暴露在售商品
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
public interface GoodsQueryService {

    /**
     * 分页查询在售商品，支持关键词与分类过滤。
     * @param dto 分页查询入参
     * @return 在售商品列表分页
     */
    IPage<SpuListVO> pageOnSale(GoodsPageDTO dto);

    /**
     * 查询商品详情，聚合 SPU 基础信息与其下 SKU 列表；非在售商品对用户不可见。
     * @param spuId SPU 主键 ID
     * @return 商品详情视图
     */
    SpuDetailVO getDetail(Long spuId);

    /**
     * 查询指定 SPU 下的 SKU 列表；非在售 SPU 对用户不可见。
     * @param spuId SPU 主键 ID
     * @return SKU 视图列表
     */
    List<SkuVO> listSkus(Long spuId);
}
