package com.yirancrazy.minimall.goods.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品只读查询服务实现，固定检索在售 SPU 并聚合 SKU
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Slf4j
@Service
public class GoodsQueryServiceImpl implements GoodsQueryService {

    private final SpuManager spuManager;
    private final SkuManager skuManager;

    public GoodsQueryServiceImpl(SpuManager spuManager, SkuManager skuManager) {
        this.spuManager = spuManager;
        this.skuManager = skuManager;
    }

    /**
     * 分页查询在售商品，keyword 非空时按标题模糊匹配，categoryId 非空时等值过滤。
     * @param dto 分页查询入参
     * @return 在售商品列表分页
     */
    @Override
    public IPage<SpuListVO> pageOnSale(GoodsPageDTO dto) {
        Page<SpuPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        IPage<SpuPO> spuPage = spuManager.page(page, Wrappers.lambdaQuery(SpuPO.class)
            .eq(SpuPO::getStatus, SpuStatusEnum.ON_SALE.statusValue())
            .eq(dto.getCategoryId() != null, SpuPO::getCategoryId, dto.getCategoryId())
            .like(dto.getKeyword() != null && !dto.getKeyword().isBlank(),
                SpuPO::getTitle, dto.getKeyword()));
        return spuPage.convert(po -> new SpuListVO(po.getId(), po.getSpuNo(), po.getTitle(),
            po.getSubtitle(), po.getMainImageUrl(), po.getMerchantId()));
    }

    /**
     * 查询商品详情，非在售 SPU 视为不存在，聚合其下全部 SKU。
     * @param spuId SPU 主键 ID
     * @return 商品详情视图
     */
    @Override
    public SpuDetailVO getDetail(Long spuId) {
        SpuPO po = spuManager.getById(spuId);
        if (po == null || po.getStatus() == null
            || po.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        List<SkuVO> skus = listSkusInternal(spuId);
        log.info("goods detail queried, spuId={}, skuCount={}", spuId, skus.size());
        return new SpuDetailVO(po.getId(), po.getSpuNo(), po.getTitle(), po.getSubtitle(),
            po.getMainImageUrl(), po.getMerchantId(), po.getCategoryId(),
            Boolean.TRUE, skus);
    }

    /**
     * 查询指定 SPU 下的 SKU 列表，非在售 SPU 视为不存在。
     * @param spuId SPU 主键 ID
     * @return SKU 视图列表
     */
    @Override
    public List<SkuVO> listSkus(Long spuId) {
        SpuPO po = spuManager.getById(spuId);
        if (po == null || po.getStatus() == null
            || po.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        return listSkusInternal(spuId);
    }

    private List<SkuVO> listSkusInternal(Long spuId) {
        List<SkuPO> skuPos = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, spuId));
        return skuPos.stream()
            .map(s -> new SkuVO(s.getId(), s.getSkuName(), s.getPrice(), s.getStock()))
            .collect(Collectors.toList());
    }
}
