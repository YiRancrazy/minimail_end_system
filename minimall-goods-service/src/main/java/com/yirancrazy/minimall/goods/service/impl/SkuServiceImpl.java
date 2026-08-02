package com.yirancrazy.minimall.goods.service.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.SkuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务实现，实现Sku相关业务逻辑
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Service
public class SkuServiceImpl implements SkuService {

    private final SkuManager skuManager;

    public SkuServiceImpl(SkuManager skuManager) {
        this.skuManager = skuManager;
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
     * 创建 SKU 记录，并对价格、库存等字段做缺省值兜底后落库。
     *
     * @param dto 待保存的 SKU 信息
     * @return 新建 SKU 的主键 ID
     */
    @Override
    public Long create(SkuCreateDTO dto) {
        SkuPO sku = new SkuPO();
        sku.setSpuId(dto.getSpuId());
        sku.setSkuName(dto.getSkuName());
        sku.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        sku.setStock(dto.getStock() != null ? dto.getStock() : 0);
        skuManager.save(sku);
        return sku.getId();
    }

    /**
     * 分页查询 SKU，skuName 非空时按 like 模糊匹配。
     *
     * @param dto 分页查询入参
     * @return SKU 分页结果
     */
    @Override
    public IPage<SkuPO> page(SkuPageDTO dto) {
        Page<SkuPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return skuManager.page(page, Wrappers.lambdaQuery(SkuPO.class)
            .like(dto.getSkuName() != null && !dto.getSkuName().isBlank(),
                SkuPO::getSkuName, dto.getSkuName()));
    }

    /**
     * 按主键更新 SKU，不存在时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @param dto 待更新的 SKU 信息
     * @return 更新是否成功
     */
    @Override
    public boolean update(Long id, SkuUpdateDTO dto) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        existing.setSkuName(dto.getSkuName());
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        if (dto.getStock() != null) {
            existing.setStock(dto.getStock());
        }
        return skuManager.updateById(existing);
    }

    /**
     * 按主键删除 SKU，不存在时抛出 SKU_NOT_FOUND。
     *
     * @param id SKU 主键 ID
     * @return 删除是否成功
     */
    @Override
    public boolean delete(Long id) {
        SkuPO existing = skuManager.getById(id);
        if (existing == null) {
            throw new BizException(SkuCodeEnum.SKU_NOT_FOUND);
        }
        return skuManager.removeById(id);
    }
}