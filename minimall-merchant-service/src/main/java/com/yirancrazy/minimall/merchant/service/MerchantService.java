package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质领域服务接口，定义资质提交与审核契约。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
public interface MerchantService {

    /**
     * 商家提交或更新资质，审核状态置为 PENDING。
     * @param merchantId 商家ID（即 user_id，由可信 Header 注入）
     * @param dto 资质提交入参
     * @return 资质VO
     */
    MerchantQualificationVO submitQualification(Long merchantId, QualificationSubmitDTO dto);

    /**
     * 查询商家资质，不存在时抛出 MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID
     * @return 资质VO
     */
    MerchantQualificationVO getQualification(Long merchantId);

    /**
     * 平台审核商家资质，仅允许 PENDING 状态审核；approved=true 置 APPROVED，false 置 REJECTED 并记录原因。
     * @param merchantId 商家主体ID
     * @param approved 是否通过
     * @param reason 驳回原因，approved=false 时填写
     */
    void audit(Long merchantId, boolean approved, String reason);
}
