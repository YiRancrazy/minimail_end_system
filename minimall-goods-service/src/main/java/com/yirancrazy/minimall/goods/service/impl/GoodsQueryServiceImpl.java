package com.yirancrazy.minimall.goods.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.GoodsPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.GoodsQueryService;
import com.yirancrazy.minimall.goods.vo.SkuVO;
import com.yirancrazy.minimall.goods.vo.SpuDetailVO;
import com.yirancrazy.minimall.goods.vo.SpuListVO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: GoodsQueryServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class GoodsQueryServiceImpl implements GoodsQueryService {

    private final SpuManager spuManager;
    private final SkuManager skuManager;
    private final SpuSearchService spuSearchService;
    private final MinioUtil minioUtil;

    /**
     * 构造 GoodsQueryServiceImpl 实例。
     * @param spuManager spuManager 参数
     * @param skuManager skuManager 参数
     * @param spuSearchService spuSearchService 参数
     * @param minioUtil minioUtil 参数
     */
    public GoodsQueryServiceImpl(SpuManager spuManager, SkuManager skuManager,
                                 SpuSearchService spuSearchService, MinioUtil minioUtil) {
        this.spuManager = spuManager;
        this.skuManager = skuManager;
        this.spuSearchService = spuSearchService;
        this.minioUtil = minioUtil;
    }

    /**
     * 执行 search 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    @Override
    public List<SpuSearchVO> search(SpuSearchDTO dto) {
        return spuSearchService.search(dto);
    }


    /**
     * 转换或解析ImageUrl。
     * @param objectKey objectKey
     * @return 处理结果
     */
    private String resolveImageUrl(String objectKey) {
        return minioUtil.resolvePublicUrl(objectKey);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<SpuListVO> pageOnSale(GoodsPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());  // 上一页最后一条记录的id
        int limit = dto.getLimit();  // 每页数量
        List<SpuPO> records = spuManager.list(Wrappers.lambdaQuery(SpuPO.class)
            .lt(lastId != null, SpuPO::getId, lastId)  // 上一页最后一条记录的id
            .eq(SpuPO::getStatus, SpuStatusEnum.ON_SALE.statusValue())  // 在售状态
            .eq(dto.getCategoryId() != null, SpuPO::getCategoryId, dto.getCategoryId())  // 类目过滤
            .like(dto.getKeyword() != null && !dto.getKeyword().isBlank(),  // 标题模糊匹配
                SpuPO::getTitle, dto.getKeyword())  // 标题
            .orderByDesc(SpuPO::getId)
            .last("LIMIT " + (limit + 1)));
        Map<Long, BigDecimal> minPriceMap = batchMinPrice(records.stream()
            .map(SpuPO::getId).collect(Collectors.toList()));
        return CursorPageVO.of(records, limit, SpuPO::getId)
            .map(po -> new SpuListVO(po.getId(), po.getSpuNo(), po.getTitle(),
                po.getSubtitle(), resolveImageUrl(po.getMainImageUrl()), po.getMerchantId(),
                minPriceMap.get(po.getId())));
    }


    /**
     * 处理batchMin价格。
     * @param spuIds SPUIds
     * @return 结果映射
     */
    private Map<Long, BigDecimal> batchMinPrice(List<Long> spuIds) {
        if (spuIds == null || spuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SkuPO> skus = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).in(SkuPO::getSpuId, spuIds));
        // 空价 SKU 不参与比价，避免 naturalOrder 对 null 抛 NPE（价格列理论非空，防御脏数据）
        return skus.stream()
            .filter(sku -> sku.getPrice() != null)
            .collect(Collectors.groupingBy(SkuPO::getSpuId,
                Collectors.mapping(SkuPO::getPrice,
                    Collectors.minBy(Comparator.naturalOrder()))))
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().orElse(null)));
    }


    /**
     * 获取相关数据。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    @Override
    public SpuDetailVO getDetail(Long spuId) {
        SpuPO po = spuManager.getById(spuId);
        if (po == null || po.getStatus() == null
            || po.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        List<SkuVO> skus = listSkusInternal(spuId);
        log.info("商品详情查询完成，spuId={}，skuCount={}", spuId, skus.size());
        return new SpuDetailVO(po.getId(), po.getSpuNo(), po.getTitle(), po.getSubtitle(),
            resolveImageUrl(po.getMainImageUrl()), po.getMerchantId(), po.getCategoryId(),
            Boolean.TRUE, skus);
    }


    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
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


    /**
     * 查询SkusInternal。
     * @param spuId SPU ID
     * @return 结果列表
     */
    private List<SkuVO> listSkusInternal(Long spuId) {
        List<SkuPO> skuPos = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, spuId));
        return skuPos.stream()
            .map(SkuVO::from)
            .collect(Collectors.toList());
    }
}
