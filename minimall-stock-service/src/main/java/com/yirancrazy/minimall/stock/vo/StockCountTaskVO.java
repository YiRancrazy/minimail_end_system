package com.yirancrazy.minimall.stock.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockCountTaskVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockCountTaskVO {

    private Long id;
    private Long skuId;
    private Long expectedQuantity;
    private Long actualQuantity;
    private Long diffQuantity;
    private Integer status;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static StockCountTaskVO from(StockCountTaskPO po) {
        StockCountTaskVO vo = new StockCountTaskVO();
        vo.setId(po.getId());
        vo.setSkuId(po.getSkuId());
        vo.setExpectedQuantity(po.getExpectedQuantity());
        vo.setActualQuantity(po.getActualQuantity());
        vo.setDiffQuantity(po.getDiffQuantity());
        vo.setStatus(po.getStatus());
        vo.setRemark(po.getRemark());
        vo.setOperatorId(po.getOperatorId());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
