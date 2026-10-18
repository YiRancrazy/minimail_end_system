package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class ComplaintVO {

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
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static ComplaintVO from(ComplaintPO po) {
        ComplaintVO vo = new ComplaintVO();
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
