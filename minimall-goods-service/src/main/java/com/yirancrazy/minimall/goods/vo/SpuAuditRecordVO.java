package com.yirancrazy.minimall.goods.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuAuditRecordVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SpuAuditRecordVO {

    private Long id;
    private Long spuId;
    private Long auditorId;
    private Integer decision;
    private String reason;
    private LocalDateTime auditAt;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static SpuAuditRecordVO from(SpuAuditRecordPO po) {
        SpuAuditRecordVO vo = new SpuAuditRecordVO();
        vo.setId(po.getId());
        vo.setSpuId(po.getSpuId());
        vo.setAuditorId(po.getAuditorId());
        vo.setDecision(po.getDecision());
        vo.setReason(po.getReason());
        vo.setAuditAt(po.getAuditAt());
        return vo;
    }
}
