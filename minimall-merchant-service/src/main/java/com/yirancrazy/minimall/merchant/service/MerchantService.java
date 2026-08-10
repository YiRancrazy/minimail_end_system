package com.yirancrazy.minimall.merchant.service;

import java.util.List;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDTO;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质领域服务接口，定义资质提交与审核契约。
 * @Version: 1.1
 * @DateTime: 2026/08/10
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

    /**
     * 商家获取自身信息，不存在时抛出 MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID（即 userId，由可信 Header 注入）
     * @return 商家信息VO
     */
    MerchantInfoVO getMerchantInfo(Long merchantId);

    /**
     * 商家查看审核记录，包含资质审核与商品审核日志。
     * @param merchantId 商家ID
     * @return 审核记录列表
     */
    List<MerchantAuditLogVO> listAuditLog(Long merchantId);

    /**
     * 平台商家管理分页查询。
     * @param dto 分页入参
     * @return 商家游标分页结果
     */
    CursorPageVO<MerchantPO> page(MerchantPageDTO dto);

    /**
     * 平台商家详情。
     * @param merchantId 商家主体ID
     * @return 商家PO
     * @throws com.yirancrazy.minimall.common.exception.BizException 商家不存在时
     */
    MerchantPO detail(Long merchantId);
}
