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
    private final MinioUtil minioUtil;

    public GoodsQueryServiceImpl(SpuManager spuManager, SkuManager skuManager, MinioUtil minioUtil) {
        this.spuManager = spuManager;
        this.skuManager = skuManager;
        this.minioUtil = minioUtil;
    }

    /**
     * 图片 objectKey 统一转可访问 URL，避免用户端拿到裸 objectKey 导致图片不可显示。
     * @param objectKey MinIO 对象键
     * @return 可访问的图片 URL；空值返回 null
     */
    private String resolveImageUrl(String objectKey) {
        return minioUtil.resolvePublicUrl(objectKey);
    }

    /**
     * 游标分页查询在售商品，keyword 非空时按标题模糊匹配，categoryId 非空时等值过滤，按 ID 倒序。
     * 列表聚合各 SPU 最低售价，避免前端逐卡调详情形成 N+1。
     * @param dto 游标分页查询入参
     * @return 在售商品游标分页结果，含 minPrice（元）
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
     * 批量查询多个 SPU 的最低售价，一次 IN 查询替代逐 SPU 的 N 次详情请求。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> 最低售价（元）；无 SKU 或全空价 SKU 的 SPU 不在结果中（调用方按 null 兜底）
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
            resolveImageUrl(po.getMainImageUrl()), po.getMerchantId(), po.getCategoryId(),
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

    /**
     * 内部方法，查询指定 SPU 下的 SKU 列表，非在售 SPU 视为不存在。
     * @param spuId SPU 主键 ID
     * @return SKU 视图列表
     */
    private List<SkuVO> listSkusInternal(Long spuId) {
        List<SkuPO> skuPos = skuManager.list(
            Wrappers.lambdaQuery(SkuPO.class).eq(SkuPO::getSpuId, spuId));
        return skuPos.stream()
            .map(SkuVO::from)
            .collect(Collectors.toList());
    }
}
