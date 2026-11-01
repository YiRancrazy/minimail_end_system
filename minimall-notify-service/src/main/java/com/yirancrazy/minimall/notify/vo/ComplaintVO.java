package com.yirancrazy.minimall.notify.vo;

import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉对外视图，提供投诉双方、关联订单、投诉内容、当前状态及平台处理结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class ComplaintVo {

    private Long id;
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
    private LocalDateTime createTime;

    /**
     * 将投诉实体字段复制为对外视图，不改变状态或处理结果语义.
     *
     * @param po 投诉实体
     * @return 投诉视图
     */
    public static ComplaintVo from(ComplaintPo po) {
        ComplaintVo vo = new ComplaintVo();
        vo.setId(po.getId());
        vo.setComplainantType(po.getComplainantType());
        vo.setComplainantId(po.getComplainantId());
        vo.setDefendantType(po.getDefendantType());
        vo.setDefendantId(po.getDefendantId());
        vo.setOrderNo(po.getOrderNo());
        vo.setComplaintType(po.getComplaintType());
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setStatus(po.getStatus());
        vo.setHandlerId(po.getHandlerId());
        vo.setHandlerResult(po.getHandlerResult());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
