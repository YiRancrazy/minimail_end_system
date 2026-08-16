package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Sku相关业务契约
 * @Version: 1.2
 * @DateTime: 2026/08/04
 */
public interface SkuService {
    /**
     * 根据ID查询SKU。
     * @param id SKU ID
     * @return SKU PO
     */
    SkuPO getById(Long id);

    /**
     * 根据ID查询SKU，并校验归属商家，供商家端使用。
     * @param id SKU ID
     * @param merchantId 商家ID，来自可信Header
     * @return SKU PO
     * @throws BizException 当 SKU 不存在或不属于该商家时
     */
    SkuPO getById(Long id, Long merchantId);

    /**
     * 创建SKU，归属商家ID来自可信Header。
     * @param merchantId 商家ID，来自可信Header
     * @param dto SKU创建DTO
     * @return SKU ID
     */
    Long create(Long merchantId, SkuCreateDTO dto);

    /**
     * 游标分页查询SKU，支持按名称模糊搜索、按归属商家过滤。
     * @param dto 游标分页查询入参
     * @return SKU 游标分页结果
     */
    CursorPageVO<SkuPO> page(SkuPageDTO dto);

    /**
     * 根据ID更新SKU信息。
     * @param id SKU ID
     * @param merchantId 商家ID，来自可信Header
     * @param dto SKU修改DTO
     * @return 更新是否成功
     */
    boolean update(Long id, Long merchantId, SkuUpdateDTO dto);

    /**
     * 根据ID删除SKU。
     * @param id SKU ID
     * @param merchantId 商家ID，来自可信Header
     * @return 删除是否成功
     */
    boolean delete(Long id, Long merchantId);
}
