package com.yirancrazy.minimall.goods.service;

import java.util.List;
import java.util.Map;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.vo.SpuVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品领域服务接口，定义Spu相关业务契约
 * @Version: 1.3
 * @DateTime: 2026/08/04
 */
public interface SpuService {

    /**
     * 根据ID查询SPU。
     * @param id SPU ID
     * @return SPU PO
     */
    SpuPO getById(Long id);

    /**
     * 根据ID查询SPU，并校验归属商家，供商家端使用。
     * @param id SPU ID
     * @param merchantId 商家ID，来自可信Header
     * @return SPU PO
     * @throws BizException 当 SPU 不存在或不属于该商家时
     */
    SpuPO getById(Long id, Long merchantId);

    /**
     * 创建SPU，初始状态为草稿。
     * @param merchantId 商家ID，来自可信Header
     * @param dto SPU创建DTO
     * @return SPU ID
     */
    Long create(Long merchantId, SpuCreateDTO dto);

    /**
     * 游标分页查询SPU，支持按商家、状态、标题过滤，并批量装配各 SPU 的 SKU 列表。
     * @param dto 游标分页查询入参
     * @return SPU 游标分页结果，含 skus
     */
    CursorPageVO<SpuVO> page(SpuPageDTO dto);

    /**
     * 根据ID更新SPU信息，字段为空表示不更新。
     * @param id SPU ID
     * @param merchantId 商家ID，来自可信Header
     * @param dto SPU修改DTO
     * @return 更新是否成功
     */
    boolean update(Long id, Long merchantId, SpuUpdateDTO dto);

    /**
     * 根据ID删除SPU。
     * @param id SPU ID
     * @param merchantId 商家ID，来自可信Header
     * @return 删除是否成功
     */
    boolean delete(Long id, Long merchantId);

    /**
     * 商家提交上架审核，仅草稿/下架/驳回状态可提交，提交后进入待审核。
     * @param id SPU ID
     * @param merchantId 商家ID，来自可信Header
     * @return 提交是否成功
     */
    boolean onShelf(Long id, Long merchantId);

    /**
     * 下架SPU，仅在售状态可下架。
     * @param id SPU ID
     * @param merchantId 商家ID，来自可信Header
     * @return 下架是否成功
     */
    boolean offShelf(Long id, Long merchantId);

    /**
     * 平台游标分页查询待审核 SPU。
     * @param dto 游标分页查询入参
     * @return 待审核 SPU 游标分页结果
     */
    CursorPageVO<SpuPO> pagePending(SpuPageDTO dto);

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

    /**
     * 重新同步 SPU 文档到 ES 索引（含 SKU 价格区间聚合），供 SKU 新增/改价/删除后刷新父 SPU 搜索数据。
     * @param spuId SPU 主键 ID
     */
    void refreshEsDocument(Long spuId);

    /**
     * 全量重灌 ES 索引：遍历全部在售 SPU 重新同步文档，用于索引重建后补齐数据。
     * 幂等，单条失败由 syncToEs 内部捕获，不影响其余 SPU。
     */
    void rebuildAllEsDocuments();

    /**
     * 批量查询 SPU 快照，供购物车列表等跨服务链路一次调用替代逐 SPU 的 N 次请求；不存在的 SPU 不放入结果。
     * @param spuIds SPU 主键集合，允许为空
     * @return spuId -> SPU 快照，空入参返回空 Map
     */
    Map<Long, SpuSnapshotDTO> listSnapshots(List<Long> spuIds);
}
