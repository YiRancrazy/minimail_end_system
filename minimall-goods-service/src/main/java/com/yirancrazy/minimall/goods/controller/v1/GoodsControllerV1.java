package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/goods")
@RequiredArgsConstructor
public class GoodsControllerV1 {

    private final GoodsQueryService goodsQueryService;


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<SpuListVO>> page(@Valid GoodsPageDTO dto) {
        return Result.success(goodsQueryService.pageOnSale(dto));
    }


    /**
     * 执行 detail 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{spuId}")
    public Result<SpuDetailVO> detail(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.getDetail(spuId));
    }


    /**
     * 执行 skus 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{spuId}/skus")
    public Result<List<SkuVO>> skus(@PathVariable("spuId") Long spuId) {
        return Result.success(goodsQueryService.listSkus(spuId));
    }


    /**
     * 执行 search 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/search")
    public Result<List<SpuSearchVO>> search(@Valid SpuSearchDTO dto) {
        return Result.success(goodsQueryService.search(dto));
    }
}
