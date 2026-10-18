package com.yirancrazy.minimall.stock.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockTransferVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockTransferVO {

    private Long id;
    private Long fromSkuId;
    private Long toSkuId;
    private Long quantity;
    private String reason;
    private Long operatorId;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static StockTransferVO from(StockTransferPO po) {
        StockTransferVO vo = new StockTransferVO();
        vo.setId(po.getId());
        vo.setFromSkuId(po.getFromSkuId());
        vo.setToSkuId(po.getToSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setReason(po.getReason());
        vo.setOperatorId(po.getOperatorId());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
