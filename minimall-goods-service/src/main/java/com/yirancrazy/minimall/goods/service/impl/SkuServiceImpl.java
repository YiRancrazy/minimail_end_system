package com.yirancrazy.minimall.goods.service.impl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SkuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;
    private final SpuManager spuManager;
    private final SpuService spuService;
    private final GoodsRemoteGateway goodsRemoteGateway;

    /**
     * 构造 SkuServiceImpl 实例。
     * @param skuManager skuManager 参数
     * @param spuManager spuManager 参数
     * @param spuService spuService 参数
     * @param goodsRemoteGateway goodsRemoteGateway 参数
     */
    public SkuServiceImpl(SkuManager skuManager, SpuManager spuManager, SpuService spuService,
                          GoodsRemoteGateway goodsRemoteGateway) {
        this.skuManager = skuManager;
        this.spuManager = spuManager;
        this.spuService = spuService;
        this.goodsRemoteGateway = goodsRemoteGateway;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    public SkuPO getById(Long id) {
        SkuPO s = skuManager.getById(id);
        if (s == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        return s;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    public SkuPO getById(Long id, Long merchantId) {
        SkuPO s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public SkuVO getVO(Long id, Long merchantId) {
        return SkuVO.from(getById(id, merchantId));
    }


    /**
     * 校验Owner。
     * @param existing existing
     * @param merchantId 商户 ID
     * @throws BizException 业务校验失败时抛出
     */
    private void checkOwner(SkuPO existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long create(Long merchantId, SkuCreateDTO dto) {
        SpuPO spu = spuManager.getById(dto.getSpuId());
        if (spu == null || spu.getMerchantId() == null || !spu.getMerchantId().equals(merchantId)) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        SkuPO sku = new SkuPO();
        sku.setSpuId(dto.getSpuId());
        sku.setMerchantId(merchantId);
        sku.setSkuName(dto.getSkuName());
        sku.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        sku.setStock(dto.getStock() != null ? dto.getStock() : 0);
        skuManager.save(sku);
        initStockBestEffort(sku, merchantId);
        // 新 SKU 可能改变父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
        spuService.refreshEsDocument(dto.getSpuId());
        return sku.getId();
    }


    /**
     * 初始化库存BestEffort。
     * @param sku SKU
     * @param merchantId 商户 ID
     */
    private void initStockBestEffort(SkuPO sku, Long merchantId) {
        try {
            Result<Void> r = goodsRemoteGateway.initStock(
                new StockInitDTO(sku.getId(), merchantId, sku.getStock()));
            if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
                log.warn("初始化库存失败，skuId={}，msg={}", sku.getId(),
                    r == null ? "无响应" : r.getMessage());
            }
        }
        catch (Exception e) {
            log.warn("初始化库存异常，skuId={}", sku.getId(), e);
        }
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<SkuPO> page(SkuPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<SkuPO> records = skuManager.list(Wrappers.lambdaQuery(SkuPO.class)
            .eq(dto.getMerchantId() != null, SkuPO::getMerchantId, dto.getMerchantId())
            .lt(lastId != null, SkuPO::getId, lastId)
            .like(dto.getSkuName() != null && !dto.getSkuName().isBlank(),
                SkuPO::getSkuName, dto.getSkuName())
            .orderByDesc(SkuPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, SkuPO::getId);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<SkuVO> pageVO(SkuPageDTO dto) {
        return page(dto).map(SkuVO::from);
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean update(Long id, Long merchantId, SkuUpdateDTO dto) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        existing.setSkuName(dto.getSkuName());
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        Integer originalStock = existing.getStock();
        Integer stockDelta = null;
        if (dto.getStock() != null && !dto.getStock().equals(existing.getStock())) {
            int currentStock = existing.getStock() == null ? 0 : existing.getStock();
            int delta = dto.getStock() - currentStock;
            stockDelta = delta;
            adjustStock(existing.getId(), merchantId, delta, "商品规格库存调整");
            existing.setStock(dto.getStock());
        }
        boolean ok = skuManager.updateById(existing);
        if (!ok && stockDelta != null) {
            existing.setStock(originalStock);
            rollbackStockAdjustment(existing.getId(), merchantId, -stockDelta);
            throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
        }
        if (ok) {
            // 改价会影响父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
            spuService.refreshEsDocument(existing.getSpuId());
        }
        return ok;
    }


    /**
     * 更新库存。
     * @param skuId SKU ID
     * @param merchantId 商户 ID
     * @param delta delta
     * @param reason 原因
     * @throws BizException 业务校验失败时抛出
     */
    private void adjustStock(Long skuId, Long merchantId, long delta, String reason) {
        Result<Void> result = goodsRemoteGateway.adjustStock(
            new StockAdjustDTO(skuId, merchantId, delta, reason));
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            throw new BizException(SkuCodeEnum.STOCK_ADJUST_FAILED);
        }
    }


    /**
     * 处理rollback库存Adjustment。
     * @param skuId SKU ID
     * @param merchantId 商户 ID
     * @param delta delta
     */
    private void rollbackStockAdjustment(Long skuId, Long merchantId, long delta) {
        try {
            adjustStock(skuId, merchantId, delta, "商品规格库存调整回滚");
        }
        catch (Exception e) {
            log.error("回滚库存调整失败，skuId={}，quantity={}", skuId, delta, e);
        }
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    @Override
    public boolean delete(Long id, Long merchantId) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        checkOwner(existing, merchantId);
        Long spuId = existing.getSpuId();
        boolean ok = skuManager.removeById(id);
        if (ok) {
            // 删除 SKU 可能改变父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
            spuService.refreshEsDocument(spuId);
        }
        return ok;
    }


    /**
     * 查询列表数据。
     * @param skuIds skuIds 参数
     * @return 结果映射
     */
    @Override
    public Map<Long, SkuSnapshotDTO> listSnapshots(List<Long> skuIds) {
        List<Long> ids = skuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SkuPO> skus = skuManager.list(Wrappers.lambdaQuery(SkuPO.class).in(SkuPO::getId, ids));
        if (skus.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> spuIds = skus.stream().map(SkuPO::getSpuId).filter(Objects::nonNull).distinct()
            .collect(Collectors.toList());
        Map<Long, SpuPO> spuMap = spuIds.isEmpty() ? Collections.emptyMap()
            : spuManager.list(Wrappers.lambdaQuery(SpuPO.class).in(SpuPO::getId, spuIds)).stream()
                .collect(Collectors.toMap(SpuPO::getId, Function.identity()));
        Map<Long, SkuSnapshotDTO> snapshots = new HashMap<>(ids.size() * 2);
        for (SkuPO s : skus) {
            SpuPO spu = s.getSpuId() == null ? null : spuMap.get(s.getSpuId());
            // 非在售/孤儿 SPU 的 SKU 不返回，购物车侧展示降级，防止未过审商品被下单
            if (spu == null || spu.getStatus() == null
                || spu.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
                continue;
            }
            snapshots.put(s.getId(), new SkuSnapshotDTO(s.getId(), s.getSpuId(), s.getSkuName(),
                s.getPrice(), s.getStock(), spu.getMerchantId()));
        }
        return snapshots;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public SkuSnapshotDTO getSnapshot(Long id) {
        SkuPO sku = getById(id);
        SpuPO spu = spuManager.getById(sku.getSpuId());
        if (spu == null) {
            throw new BizException(SpuCodeEnum.SPU_NOT_FOUND);
        }
        if (spu.getStatus() == null || spu.getStatus() != SpuStatusEnum.ON_SALE.statusValue()) {
            throw new BizException(SpuCodeEnum.SPU_NOT_ON_SALE);
        }
        return new SkuSnapshotDTO(sku.getId(), sku.getSpuId(), sku.getSkuName(),
            sku.getPrice(), sku.getStock(), spu.getMerchantId());
    }
}
