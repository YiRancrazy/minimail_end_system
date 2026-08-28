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
import com.yirancrazy.minimall.api.dto.stock.StockInitDTO;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
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
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Sku相关业务逻辑
 * @Version: 1.3
 * @DateTime: 2026/08/02
 */
@Slf4j
@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;
    private final SpuManager spuManager;
    private final SpuService spuService;
    private final StockFeignClient stockFeignClient;

    public SkuServiceImpl(SkuManager skuManager, SpuManager spuManager, SpuService spuService,
                          StockFeignClient stockFeignClient) {
        this.skuManager = skuManager;
        this.spuManager = spuManager;
        this.spuService = spuService;
        this.stockFeignClient = stockFeignClient;
    }

    /**
     * 按主键查询 SKU，不存在时抛出 SKU_NOT_FOUND 业务异常。
     *
     * @param id SKU 主键 ID
     * @return 已存在的 SKU 实体
     */
    @Override
    public SkuPO getById(Long id) {
        SkuPO s = skuManager.getById(id);
        if (s == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        return s;
    }

    /**
     * 按主键查询 SKU 并校验归属商家，供商家端使用；非本人商品按"不存在"返回，避免越权信息泄露。
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 属于该商家的 SKU 实体
     */
    @Override
    public SkuPO getById(Long id, Long merchantId) {
        SkuPO s = getById(id);
        checkOwner(s, merchantId);
        return s;
    }

    /**
     * 归属校验：SKU 归属商家与请求商家不一致时抛出 SKU_NOT_FOUND，防越权的同时不暴露资源存在性。
     * @param existing 已查出的 SKU 实体
     * @param merchantId 请求方商家ID
     */
    private void checkOwner(SkuPO existing, Long merchantId) {
        if (existing.getMerchantId() != null && !existing.getMerchantId().equals(merchantId)) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
    }

    /**
     * 创建 SKU 记录，归属商家ID来自可信Header；父 SPU 不存在或非本人商品时按"不存在"处理，
     * 防止商家在他人 SPU 下挂载 SKU。价格、库存等字段做缺省值兜底后落库。
     *
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待保存的 SKU 信息
     * @return 新建 SKU 的主键 ID
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
     * 联动初始化 SKU 库存记录：stock-service 异常或降级时按 WARN 记录不阻断创建，
     * 后续商家可手动入库兜底。
     * @param sku 已落库的新建 SKU
     * @param merchantId 商家ID，来自可信Header
     */
    private void initStockBestEffort(SkuPO sku, Long merchantId) {
        try {
            Result<Void> r = stockFeignClient.initStock(
                new StockInitDTO(sku.getId(), merchantId, sku.getStock()));
            if (r == null || !CommonCode.SUCCESS.equals(r.getCode())) {
                log.warn("init stock failed, skuId={}, msg={}", sku.getId(),
                    r == null ? "no response" : r.getMessage());
            }
        }
        catch (Exception e) {
            log.warn("init stock error, skuId={}", sku.getId(), e);
        }
    }

    /**
     * 游标分页查询 SKU，按归属商家过滤，skuName 非空时按 like 模糊匹配，按 ID 倒序。
     *
     * @param dto 游标分页查询入参
     * @return SKU 游标分页结果
     */
    @Override
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
     * 按主键更新 SKU，不存在或非本人商品时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @param dto 待更新的 SKU 信息
     * @return 更新是否成功
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
        if (dto.getStock() != null) {
            existing.setStock(dto.getStock());
        }
        boolean ok = skuManager.updateById(existing);
        if (ok) {
            // 改价会影响父 SPU 价格区间，刷新 ES 镜像（失败不阻断主链路）
            spuService.refreshEsDocument(existing.getSpuId());
        }
        return ok;
    }

    /**
     * 按主键删除 SKU，不存在或非本人商品时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @param merchantId 商家ID，来自可信Header
     * @return 删除是否成功
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
     * 批量查询 SKU 快照：一次 IN 查询 SKU、一次 IN 查询所属 SPU，替代逐 SKU 的 N 次请求；
     * 仅返回所属 SPU 在售的 SKU，与单条快照的防越权/防未过审语义保持一致，缺失项由调用方兜底降级。
     * @param skuIds SKU 主键集合，允许为空
     * @return skuId -> SKU 快照，空入参返回空 Map
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
}