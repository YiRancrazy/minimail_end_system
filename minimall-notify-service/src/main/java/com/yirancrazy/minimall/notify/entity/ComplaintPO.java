package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉持久化对象，映射 t_notify_complaint 表
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_complaint")
public class ComplaintPO extends BasePO {

    /** 投诉方类型 1=用户 2=商家 */
    private Integer complainantType;

    /** 投诉方ID */
    private Long complainantId;

    /** 被诉方类型 1=用户 2=商家 */
    private Integer defendantType;

    /** 被诉方ID */
    private Long defendantId;

    /** 关联订单号 */
    private String orderNo;

    /** 投诉类型，见 ComplaintTypeEnum */
    private String complaintType;

    /** 投诉标题 */
    private String title;

    /** 投诉内容 */
    private String content;

    /** 状态，见 ComplaintStatusEnum */
    private Integer status;

    /** 处理人ID */
    private Long handlerId;

    /** 处理结果 */
    private String handlerResult;
}
