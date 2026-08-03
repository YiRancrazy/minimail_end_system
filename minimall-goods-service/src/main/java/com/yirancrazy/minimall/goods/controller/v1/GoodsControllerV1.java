package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品查询控制器，提供在售商品列表、详情、SKU 列表与搜索接口
 * @Version: 1.1
 * @DateTime: 2026/08/03
 **/
@RestController
@RequestMapping("/api/v1/goods")
public class GoodsControllerV1 {

    private final GoodsQueryService goodsQueryService;
    private final SpuSearchService spuSearchService;

    public GoodsControllerV1(GoodsQueryService goodsQueryService,
                             SpuSearchService spuSearchService) {
        this.goodsQueryService = goodsQueryService;
        this.spuSearchService = spuSearchService;
    }

    /**
     * 分页获取在售商品列表，支持关键词搜索与分类过滤。
     * @param dto 分页查询入参
     * @return 在售商品列表分页
     */
    @GetMapping
    public Result<IPage<SpuListVO>> page(@Valid GoodsPageDTO dto) {
        return Result.success(goodsQueryService.pageOnSale(dto));
    }

    /**
     * 获取商品详情，聚合 SPU 基础信息与 SKU 列表。
     * @param spuId SPU 主键 ID
     * @return 商品详情视图
     */
    @GetMapping("/{spuId}")
    public Result<SpuDetailVO> detail(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.getDetail(spuId));
    }

    /**
     * 获取指定 SPU 下的 SKU 列表。
     * @param spuId SPU 主键 ID
     * @return SKU 视图列表
     */
    @GetMapping("/{spuId}/skus")
    public Result<List<SkuVO>> skus(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.listSkus(spuId));
    }

    /**
     * ES 商品搜索，支持关键词全文检索 + 分类/价格范围过滤。
     * @param dto 搜索入参
     * @return 搜索结果列表
     */
    @GetMapping("/search")
    public Result<List<SpuSearchVO>> search(@Valid SpuSearchDTO dto) {
        return Result.success(spuSearchService.search(dto));
    }
}
