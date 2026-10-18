package com.yirancrazy.minimall.merchant.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.merchant.entity.ShopPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class ShopVO {

    private Long id;
    private Long merchantId;
    private String shopName;
    private String licenseNo;
    private Integer status;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static ShopVO from(ShopPO po) {
        ShopVO vo = new ShopVO();
        vo.setId(po.getId());
        vo.setMerchantId(po.getMerchantId());
        vo.setShopName(po.getShopName());
        vo.setLicenseNo(po.getLicenseNo());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
