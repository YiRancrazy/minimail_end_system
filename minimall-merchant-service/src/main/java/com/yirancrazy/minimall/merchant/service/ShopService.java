package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.vo.ShopVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface ShopService {


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 处理结果
     */
    ShopPO getById(Long merchantId, Long id);
    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 处理结果
     */
    ShopVO getVO(Long merchantId, Long id);


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    ShopSnapshotDTO getSnapshot(Long id);



    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    java.util.List<ShopPO> list(Long merchantId, String cursor, Integer limit);
    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    java.util.List<ShopVO> listVO(Long merchantId, String cursor, Integer limit);


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long merchantId, ShopCreateDTO dto);


    /**
     * 更新业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long merchantId, Long id, ShopUpdateDTO dto);


    /**
     * 删除业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 是否处理成功
     */
    boolean delete(Long merchantId, Long id);
}
