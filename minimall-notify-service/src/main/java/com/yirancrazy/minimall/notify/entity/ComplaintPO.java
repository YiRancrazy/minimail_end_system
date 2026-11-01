package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_notify_complaint 投诉记录，保存投诉双方、关联订单、投诉内容、处理状态和平台处理结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_complaint")
public class ComplaintPo extends BasePo {

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
