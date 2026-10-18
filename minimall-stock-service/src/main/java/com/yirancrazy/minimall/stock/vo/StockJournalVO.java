package com.yirancrazy.minimall.stock.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockJournalVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockJournalVO {

    private Long id;
    private Long skuId;
    private Long quantity;
    private Integer type;
    private String reason;
    private String orderNo;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static StockJournalVO from(StockJournalPO po) {
        StockJournalVO vo = new StockJournalVO();
        vo.setId(po.getId());
        vo.setSkuId(po.getSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setType(po.getType());
        vo.setReason(po.getReason());
        vo.setOrderNo(po.getOrderNo());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
