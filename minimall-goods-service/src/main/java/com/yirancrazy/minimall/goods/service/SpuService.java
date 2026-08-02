package com.yirancrazy.minimall.goods.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Spu相关业务契约
 * @Version: 1.1
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
     * 商家提交上架审核，仅草稿/下架/驳回状态可提交，提交后进入待审核。
     * @param id SPU ID
     * @return 提交是否成功
     */
    boolean onShelf(Long id);

    /**
     * 下架SPU，仅在售状态可下架。
     * @param id SPU ID
     * @return 下架是否成功
     */
    boolean offShelf(Long id);

    /**
     * 平台分页查询待审核 SPU。
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 待审核 SPU 分页结果
     */
    IPage<SpuPO> pagePending(Integer pageNo, Integer pageSize);

    /**
     * 平台审核通过，将 SPU 从待审核置为在售并记录审核日志。
     * @param spuId SPU ID
     * @param auditorId 审核员账号ID，来自网关 X-User-Id
     * @return 审核是否成功
     */
    boolean approve(Long spuId, Long auditorId);

    /**
     * 平台审核驳回，将 SPU 从待审核置为驳回并记录审核日志。
     * @param spuId SPU ID
     * @param auditorId 审核员账号ID，来自网关 X-User-Id
     * @param reason 驳回原因，不可为空
     * @return 审核是否成功
     */
    boolean reject(Long spuId, Long auditorId, String reason);

    /**
     * 查询指定 SPU 的审核记录列表，按审核时间倒序。
     * @param spuId SPU ID
     * @return 审核记录列表
     */
    List<SpuAuditRecordPO> listAuditRecords(Long spuId);
}
