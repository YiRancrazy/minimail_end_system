package com.yirancrazy.minimall.merchant.vo;

import com.yirancrazy.minimall.merchant.entity.ShopPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺视图；输出店铺主键、所属商家、名称、证照号、状态和创建时间，供商家端列表与详情展示。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ShopVo {

  private Long id;
  private Long merchantId;
  private String shopName;
  private String licenseNo;
  private Integer status;
  private LocalDateTime createTime;

  /**
   * 将店铺持久化对象映射为展示视图。
   *
   * 该方法只复制字段，不访问数据库，也不校验商家归属。
   *
   * @param po 店铺持久化对象
   * @return 店铺视图。
   */
  public static ShopVo from(ShopPo po) {
    ShopVo vo = new ShopVo();
    vo.setId(po.getId());
    vo.setMerchantId(po.getMerchantId());
    vo.setShopName(po.getShopName());
    vo.setLicenseNo(po.getLicenseNo());
    vo.setStatus(po.getStatus());
    vo.setCreateTime(po.getCreateTime());
    return vo;
  }
}
