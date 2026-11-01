package com.yirancrazy.minimall.stock.vo;

import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 向平台接口输出库存调拨的来源、目标、数量和操作审计信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class StockTransferVo {

    private Long id;
    private Long fromSkuId;
    private Long toSkuId;
    private Long quantity;
    private String reason;
    private Long operatorId;
    private LocalDateTime createTime;

    /**
     * 将库存调拨持久化对象转换为接口视图。
     *
     * @param po 库存调拨持久化对象
     * @return 与调拨记录字段对应的视图对象
     */
    public static StockTransferVo from(StockTransferPo po) {
        StockTransferVo vo = new StockTransferVo();
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
