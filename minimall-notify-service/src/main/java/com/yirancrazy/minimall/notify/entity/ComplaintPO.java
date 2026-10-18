package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_complaint")
public class ComplaintPO extends BasePO {


    private Integer complainantType;


    private Long complainantId;


    private Integer defendantType;


    private Long defendantId;


    private String orderNo;


    private String complaintType;


    private String title;


    private String content;


    private Integer status;


    private Long handlerId;


    private String handlerResult;
}
