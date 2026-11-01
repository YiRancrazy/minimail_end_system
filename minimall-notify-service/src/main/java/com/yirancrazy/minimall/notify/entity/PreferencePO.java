package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_notify_preference 用户通知偏好，保存单个通知类别在站点、短信和邮件渠道上的开关.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_preference")
public class PreferencePo extends BasePo {

    private Long userId;

    private String categoryCode;

    private Integer siteEnabled;

    private Integer smsEnabled;

    private Integer emailEnabled;
}
