package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家自身信息视图对象，返回商家店铺与资质聚合信息。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class MerchantInfoVO {
    private Long merchantId;
    private String shopName;
    private String licenseNo;
    private Integer qualificationStatus;
    private String username;
    private LocalDateTime createTime;

    /**
     * 将 MerchantPO 转换为 MerchantInfoVO。
     * @param po 商家主体持久化对象
     * @return 商家信息VO
     */
    public static MerchantInfoVO from(com.yirancrazy.minimall.merchant.entity.MerchantPO po) {
        MerchantInfoVO vo = new MerchantInfoVO();
        vo.setMerchantId(po.getUserId());
        vo.setShopName(po.getMerchantName());
        vo.setLicenseNo(po.getLicenseNo());
        vo.setQualificationStatus(po.getAuditStatus());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
