package com.yirancrazy.minimall.goods.service;

import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.goods.dto.SpuCreateDto;
import com.yirancrazy.minimall.goods.dto.SpuPageDto;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVo;
import com.yirancrazy.minimall.goods.vo.SpuVo;
import java.util.List;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义 SPU 维护、上下架送审、平台审核、审核记录及搜索镜像同步能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface SpuService {

    /**
     * 读取 SPU 并校验商家归属；不存在或越权统一抛出 SPU_NOT_FOUND。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 匹配的 SPU 持久化对象
     */
    SpuPo getById(Long id, Long merchantId);

    /**
     * 按主键读取 SPU，不存在时抛出 SPU_NOT_FOUND。
     *
     * @param id SPU 主键
     * @return 匹配的 SPU 持久化对象
     */
    SpuPo getById(Long id);

    /**
     * 读取商家自有 SPU 并组装其 SKU 列表。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 转换后的 SPU 视图对象
     */
    SpuVo getDetail(Long id, Long merchantId);

    /**
     * 创建 SPU 及初始 SKU，创建前校验店铺归属和营业状态。
     *
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 创建参数
     * @return 新建记录的主键
     */
    Long create(Long merchantId, SpuCreateDto dto);

    /**
     * 按主键倒序分页查询待审核 SPU。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为匹配的 SPU 持久化对象
     */
    CursorPageVo<SpuPo> pagePending(SpuPageDto dto);

    /**
     * 按商家、状态和标题游标分页查询 SPU 并组装 SKU。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为转换后的 SPU 视图对象
     */
    CursorPageVo<SpuVo> page(SpuPageDto dto);

    /**
     * 增量更新 SPU 和 SKU；非草稿商品需要重新审核。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @param dto SPU 增量更新参数
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean update(Long id, Long merchantId, SpuUpdateDto dto);

    /**
     * 删除商家自有 SPU 并移除对应搜索镜像。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean delete(Long id, Long merchantId);

    /**
     * 将草稿、下架或驳回 SPU 提交审核，送审前重新校验店铺状态。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean onShelf(Long id, Long merchantId);

    /**
     * 将在售 SPU 下架，并同步搜索状态。
     *
     * @param id SPU 主键
     * @param merchantId 当前商家 ID，用于商品归属校验
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean offShelf(Long id, Long merchantId);

    /**
     * 分页查询待审核 SPU 并转换为视图对象。
     *
     * @param dto SPU 游标分页或状态过滤参数
     * @return 按主键游标分页的结果；记录项为转换后的 SPU 视图对象
     */
    CursorPageVo<SpuVo> pendingVo(SpuPageDto dto);

    /**
     * 按 SPU 主键查询审核记录，按审核时间倒序返回。
     *
     * @param spuId SPU 主键
     * @return 商品审核记录列表；无数据时返回空列表
     */
    List<SpuAuditRecordPo> listAuditRecords(Long spuId);

    /**
     * 将指定 SPU 主数据转换为平台审核视图。
     *
     * @param spuId SPU 主键
     * @return 转换后的 SPU 视图对象
     */
    SpuVo detailVo(Long spuId);

    /**
     * 审核通过待审 SPU，写入发布时间和审核记录。
     *
     * @param spuId SPU 主键
     * @param auditorId 平台审核人用户 ID
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean approve(Long spuId, Long auditorId);

    /**
     * 驳回待审 SPU，记录审核人和驳回原因。
     *
     * @param spuId SPU 主键
     * @param auditorId 平台审核人用户 ID
     * @param reason 驳回或库存调整原因
     * @return 操作实际影响记录时为 true，否则为 false
     */
    boolean reject(Long spuId, Long auditorId, String reason);

    /**
     * 按 SPU 主键查询审核记录并转换为视图对象。
     *
     * @param spuId SPU 主键
     * @return 商品审核记录视图对象列表；无数据时返回空列表
     */
    List<SpuAuditRecordVo> listAuditRecordsVo(Long spuId);

    /**
     * 重建单个 SPU 的 Elasticsearch 文档；SPU 已删除时忽略。
     *
     * @param spuId SPU 主键
     */
    void refreshEsDocument(Long spuId);

    /**
     * 全量重建所有在售 SPU 的 Elasticsearch 文档。
     */
    void rebuildAllEsDocuments();

    /**
     * 批量读取 SPU 快照，以 SPU 主键建立返回映射。
     *
     * @param spuIds 待查询的 SPU 主键列表，空列表返回空映射
     * @return 以 SPU 主键为键的快照映射；不存在的记录不包含
     */
    Map<Long, SpuSnapshotDto> listSnapshots(List<Long> spuIds);

    /**
     * 读取单个 SPU 快照；记录不存在时返回空标题和主图。
     *
     * @param id SPU 主键
     * @return SPU 标题和主图快照
     */
    SpuSnapshotDto getSnapshot(Long id);
}
