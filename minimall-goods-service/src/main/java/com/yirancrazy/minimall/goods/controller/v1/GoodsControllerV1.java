package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.GoodsPageDto;
import com.yirancrazy.minimall.goods.dto.SpuSearchDto;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import com.yirancrazy.minimall.goods.vo.SpuDetailVo;
import com.yirancrazy.minimall.goods.vo.SpuListVo;
import com.yirancrazy.minimall.goods.vo.SpuSearchVo;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 面向用户端提供在售商品分页、详情、SKU 列表和 Elasticsearch 搜索接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/goods")
@RequiredArgsConstructor
public class GoodsControllerV1 {

    private final GoodsQueryService goodsQueryService;

    /**
     * 分页查询在售商品，支持关键字和类目过滤，并返回游标分页结果。
     *
     * @param dto 商品游标分页参数
     * @return 统一响应包装；data 为按主键游标分页的结果；记录项为在售商品列表项
     */
    @GetMapping
    public Result<CursorPageVo<SpuListVo>> page(@Valid GoodsPageDto dto) {
        return Result.success(goodsQueryService.pageOnSale(dto));
    }

    /**
     * 查询在售商品详情；不存在或非在售由查询服务抛出业务异常。
     *
     * @param spuId SPU 主键
     * @return 统一响应包装；data 为商品详情及 SKU 视图集合
     */
    @GetMapping("/{spuId}")
    public Result<SpuDetailVo> detail(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.getDetail(spuId));
    }

    /**
     * 查询指定在售 SPU 的 SKU 列表。
     *
     * @param spuId SPU 主键
     * @return 统一响应包装；data 为转换后的 SKU 视图对象列表；无数据时返回空列表
     */
    @GetMapping("/{spuId}/skus")
    public Result<List<SkuVo>> skus(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.listSkus(spuId));
    }

    /**
     * 按关键字、类目、价格区间分页搜索在售商品。
     *
     * @param dto 关键词、类目、价格和分页搜索参数
     * @return 统一响应包装；data 为商品搜索视图对象列表；无数据时返回空列表
     */
    @GetMapping("/search")
    public Result<List<SpuSearchVo>> search(@Valid SpuSearchDto dto) {
        return Result.success(goodsQueryService.search(dto));
    }
}
