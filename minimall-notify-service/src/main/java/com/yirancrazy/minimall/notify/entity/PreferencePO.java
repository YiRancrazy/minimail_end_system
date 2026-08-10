package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好持久化对象，映射 t_notify_preference 表
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_preference")
public class PreferencePO extends BasePO {

    /** 用户ID */
    private Long userId;

    /** 通知类别编码，见 NotifyCategoryEnum */
    private String categoryCode;

    /** 站内信开关 0=关 1=开 */
    private Integer siteEnabled;

    /** 短信开关 0=关 1=开 */
    private Integer smsEnabled;

    /** 邮件开关 0=关 1=开 */
    private Integer emailEnabled;
}
