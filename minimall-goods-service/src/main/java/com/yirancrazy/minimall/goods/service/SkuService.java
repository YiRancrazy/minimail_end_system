package com.yirancrazy.minimall.goods.service;

import java.util.List;
import java.util.Map;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.vo.SkuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface SkuService {



    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    SkuPO getById(Long id, Long merchantId);
    /**
     * 获取相关数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    SkuVO getVO(Long id, Long merchantId);


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long merchantId, SkuCreateDTO dto);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SkuPO> page(SkuPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<SkuVO> pageVO(SkuPageDTO dto);


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long id, Long merchantId, SkuUpdateDTO dto);


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 是否处理成功
     */
    boolean delete(Long id, Long merchantId);


    /**
     * 查询列表数据。
     * @param skuIds skuIds 参数
     * @return 结果映射
     */
    Map<Long, SkuSnapshotDTO> listSnapshots(List<Long> skuIds);


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    SkuPO getById(Long id);
    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    SkuSnapshotDTO getSnapshot(Long id);
}
