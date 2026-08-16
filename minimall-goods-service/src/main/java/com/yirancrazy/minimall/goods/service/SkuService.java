package com.yirancrazy.minimall.goods.service;

import java.util.List;
import java.util.Map;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Sku相关业务契约
 * @Version: 1.3
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

    /**
     * 批量查询 SKU 快照，供购物车列表等跨服务链路一次调用替代逐 SKU 的 N 次请求。
     * 与单条快照语义一致：仅返回所属 SPU 在售的 SKU，非在售/孤儿 SKU 不放入结果，由调用方兜底降级。
     * @param skuIds SKU 主键集合，允许为空
     * @return skuId -> SKU 快照，空入参返回空 Map
     */
    Map<Long, SkuSnapshotDTO> listSnapshots(List<Long> skuIds);
}
