package com.yirancrazy.minimall.notify.vo;

import lombok.Data;
import com.yirancrazy.minimall.notify.entity.PreferencePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class PreferenceVO {

    private Long id;
    private String categoryCode;
    private Integer siteEnabled;
    private Integer smsEnabled;
    private Integer emailEnabled;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
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
