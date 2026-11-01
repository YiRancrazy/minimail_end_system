package com.yirancrazy.minimall.merchant.vo;

import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家概览视图；将资质摘要转换为商家端展示字段，并对手机号、身份证和银行卡执行脱敏。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class MerchantInfoVo {
    private Long merchantId;
    private String shopName;
    private String licenseNo;
    private Integer qualificationStatus;
    private String username;
    private String legalPhoneMasked;
    private String idCardNoMasked;
    private String bankAccountMasked;
    private LocalDateTime createTime;

    /**
     * 从商家持久化对象构建商家概览，并将 userId 映射为 merchantId。
     *
     * 手机号、身份证和银行卡在转换时脱敏；证照地址不做 URL 解析。
     *
     * @param po 商家持久化对象
     * @return 商家概览视图。
     */
    public static MerchantInfoVo from(MerchantPo po) {
        MerchantInfoVo vo = new MerchantInfoVo();
        vo.setMerchantId(po.getUserId());
        vo.setShopName(po.getMerchantName());
        vo.setLicenseNo(po.getLicenseNo());
        vo.setQualificationStatus(po.getAuditStatus());
        vo.setLegalPhoneMasked(SensitiveDataUtils.maskPhone(po.getLegalPhoneEnc()));
        vo.setIdCardNoMasked(SensitiveDataUtils.maskIdCard(po.getIdCardNoEnc()));
        vo.setBankAccountMasked(SensitiveDataUtils.maskBankCard(po.getBankAccountEnc()));
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
