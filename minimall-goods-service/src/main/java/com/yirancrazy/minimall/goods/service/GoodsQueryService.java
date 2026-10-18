package com.yirancrazy.minimall.goods.service;

import java.util.List;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsQueryService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface GoodsQueryService {


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SpuListVO> pageOnSale(GoodsPageDTO dto);


    /**
     * 获取相关数据。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    SpuDetailVO getDetail(Long spuId);


    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
     */
    List<SkuVO> listSkus(Long spuId);


    /**
     * 执行 search 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<SpuSearchVO> search(SpuSearchDTO dto);
}
