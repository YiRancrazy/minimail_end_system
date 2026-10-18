package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferencePO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_preference")
public class PreferencePO extends BasePO {


    private Long userId;


    private String categoryCode;


    private Integer siteEnabled;


    private Integer smsEnabled;


    private Integer emailEnabled;
}
