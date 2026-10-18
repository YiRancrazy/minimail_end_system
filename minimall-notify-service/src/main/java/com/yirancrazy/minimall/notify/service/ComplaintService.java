package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.vo.ComplaintVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface ComplaintService {


    /**
     * 创建业务数据。
     * @param complainantId complainantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long complainantId, ComplaintCreateDTO dto);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<ComplaintPO> page(ComplaintPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<ComplaintVO> pageVO(ComplaintPageDTO dto);


    /**
     * 处理业务事件。
     * @param id id 参数
     * @param handlerId handlerId 参数
     * @param dto dto 参数
     */
    void handle(Long id, Long handlerId, ComplaintHandleDTO dto);



    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @return 处理结果
     */
    ComplaintPO detail(Long id);
    /**
     * 执行 detailVO 方法。
     * @param id id 参数
     * @return 处理结果
     */
    ComplaintVO detailVO(Long id);


    /**
     * 执行 detailForUser 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    ComplaintVO detailForUser(Long id, Long userId);
}
