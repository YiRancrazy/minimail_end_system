package com.yirancrazy.minimall.notify.vo;

import lombok.Data;
import com.yirancrazy.minimall.notify.entity.PreferencePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好视图对象
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class PreferenceVO {

    private Long id;
    private String categoryCode;
    private Integer siteEnabled;
    private Integer smsEnabled;
    private Integer emailEnabled;

    /**
     * PO → VO 转换。
     * @param po 偏好持久化对象
     * @return 偏好视图对象
     */
    public static PreferenceVO from(PreferencePO po) {
        if (po == null) {
            return null;
        }
        PreferenceVO vo = new PreferenceVO();
        vo.setId(po.getId());
        vo.setCategoryCode(po.getCategoryCode());
        vo.setSiteEnabled(po.getSiteEnabled());
        vo.setSmsEnabled(po.getSmsEnabled());
        vo.setEmailEnabled(po.getEmailEnabled());
        return vo;
    }
}
