package com.yirancrazy.minimall.merchant.service;

import java.util.List;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDTO;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface MerchantService {


    /**
     * 执行 submitQualification 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    MerchantQualificationVO submitQualification(Long merchantId, QualificationSubmitDTO dto);


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    MerchantQualificationVO getQualification(Long merchantId);


    /**
     * 执行 audit 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     */
    void audit(Long merchantId, boolean approved, String reason);


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    MerchantInfoVO getMerchantInfo(Long merchantId);


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    List<MerchantAuditLogVO> listAuditLog(Long merchantId);


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<MerchantManageVO> page(MerchantPageDTO dto);


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    MerchantManageVO detail(Long merchantId);
}
