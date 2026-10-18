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
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVO;
import com.yirancrazy.minimall.goods.vo.SpuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface SpuService {




    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    SpuPO getById(Long id, Long merchantId);
    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    SpuVO getDetail(Long id, Long merchantId);


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long merchantId, SpuCreateDTO dto);


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SpuPO> pagePending(SpuPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SpuVO> page(SpuPageDTO dto);


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long id, Long merchantId, SpuUpdateDTO dto);


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    boolean delete(Long id, Long merchantId);


    /**
     * 执行 onShelf 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    boolean onShelf(Long id, Long merchantId);


    /**
     * 执行 offShelf 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    boolean offShelf(Long id, Long merchantId);



    /**
     * 执行 pendingVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SpuVO> pendingVO(SpuPageDTO dto);


    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
     */
    List<SpuAuditRecordPO> listAuditRecords(Long spuId);
    /**
     * 执行 detailVO 方法。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    SpuVO detailVO(Long spuId);


    /**
     * 执行 approve 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @return 是否处理成功
     */
    boolean approve(Long spuId, Long auditorId);


    /**
     * 执行 reject 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @param reason reason 参数
     * @return 是否处理成功
     */
    boolean reject(Long spuId, Long auditorId, String reason);



    /**
     * 查询列表数据。
     * @param spuId spuId 参数
     * @return 结果列表
     */
    List<SpuAuditRecordVO> listAuditRecordsVO(Long spuId);


    /**
     * 执行 refreshEsDocument 方法。
     * @param spuId spuId 参数
     */
    void refreshEsDocument(Long spuId);


    /**
     * 执行 rebuildAllEsDocuments 方法。
     */
    void rebuildAllEsDocuments();


    /**
     * 查询列表数据。
     * @param spuIds spuIds 参数
     * @return 结果映射
     */
    Map<Long, SpuSnapshotDTO> listSnapshots(List<Long> spuIds);


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    SpuPO getById(Long id);
    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    SpuSnapshotDTO getSnapshot(Long id);
}
