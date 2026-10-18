package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVO;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformMerchantService {


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PlatformMerchantVO> page(PlatformMerchantPageDTO dto);


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    PlatformMerchantVO detail(Long merchantId);


    /**
     * 查询列表数据。
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    CursorPageVO<MerchantQualificationVO> listPendingQualifications(String cursor, int limit);


    /**
     * 执行 auditQualification 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     */
    void auditQualification(Long merchantId, MerchantQualificationAuditDTO dto);
}
