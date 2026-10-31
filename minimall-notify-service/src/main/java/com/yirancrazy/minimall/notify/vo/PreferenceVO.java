package com.yirancrazy.minimall.notify.vo;

import com.yirancrazy.minimall.notify.entity.PreferencePo;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户通知偏好视图，展示单个通知类别在站点、短信和邮件渠道上的启用状态.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class PreferenceVo {

  private Long id;
  private String categoryCode;
  private Integer siteEnabled;
  private Integer smsEnabled;
  private Integer emailEnabled;

  /**
   * 将偏好实体转换为视图并原样复制三个渠道开关.
   *
   * @param po 偏好实体
   * @return 偏好视图；po 为 null 时返回 null
   */
  public static PreferenceVo from(PreferencePo po) {
    if (po == null) {
      return null;
    }
    PreferenceVo vo = new PreferenceVo();
    vo.setId(po.getId());
    vo.setCategoryCode(po.getCategoryCode());
    vo.setSiteEnabled(po.getSiteEnabled());
    vo.setSmsEnabled(po.getSmsEnabled());
    vo.setEmailEnabled(po.getEmailEnabled());
    return vo;
  }
}
