package com.yirancrazy.minimall.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SpuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Spu相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
public interface SpuService {

    /**
     * 根据ID查询SPU。
     * @param id SPU ID
     * @return SPU PO
     */
    SpuPO getById(Long id);

    /**
     * 创建SPU，初始状态为草稿。
     * @param merchantId 商家ID，来自可信Header
     * @param dto SPU创建DTO
     * @return SPU ID
     */
    Long create(Long merchantId, SpuCreateDTO dto);

    /**
     * 分页查询SPU，支持按商家、状态、标题过滤。
     * @param dto 分页查询入参
     * @return SPU 分页结果
     */
    IPage<SpuPO> page(SpuPageDTO dto);

    /**
     * 根据ID更新SPU信息，字段为空表示不更新。
     * @param id SPU ID
     * @param dto SPU修改DTO
     * @return 更新是否成功
     */
    boolean update(Long id, SpuUpdateDTO dto);

    /**
     * 根据ID删除SPU。
     * @param id SPU ID
     * @return 删除是否成功
     */
    boolean delete(Long id);

    /**
     * 上架SPU，仅草稿/下架/驳回状态可上架。
     * @param id SPU ID
     * @return 上架是否成功
     */
    boolean onShelf(Long id);

    /**
     * 下架SPU，仅在售状态可下架。
     * @param id SPU ID
     * @return 下架是否成功
     */
    boolean offShelf(Long id);
}
