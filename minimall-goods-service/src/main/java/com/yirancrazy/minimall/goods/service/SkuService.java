package com.yirancrazy.minimall.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Sku相关业务契约
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface SkuService {
    /**
     * 根据ID查询SKU。
     * @param id SKU ID
     * @return SKU PO
     */
    SkuPO getById(Long id);

    /**
     * 创建SKU。
     * @param dto SKU创建DTO
     * @return SKU ID
     */
    Long create(SkuCreateDTO dto);

    /**
     * 分页查询SKU，支持按名称模糊搜索。
     * @param dto 分页查询入参
     * @return SKU 分页结果
     */
    IPage<SkuPO> page(SkuPageDTO dto);

    /**
     * 根据ID更新SKU信息。
     * @param id SKU ID
     * @param dto SKU修改DTO
     * @return 更新是否成功
     */
    boolean update(Long id, SkuUpdateDTO dto);

    /**
     * 根据ID删除SKU。
     * @param id SKU ID
     * @return 删除是否成功
     */
    boolean delete(Long id);
}