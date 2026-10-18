package com.yirancrazy.minimall.stock.vo;

import lombok.Data;
import com.yirancrazy.minimall.stock.entity.StockPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class StockVO {

    private Long id;
    private Long skuId;
    private Long merchantId;
    private Long available;
    private Long reserved;
    private Long alertThreshold;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static StockVO from(StockPO po) {
        StockVO vo = new StockVO();
        vo.setId(po.getId());
        vo.setSkuId(po.getSkuId());
        vo.setMerchantId(po.getMerchantId());
        vo.setAvailable(po.getAvailable());
        vo.setReserved(po.getReserved());
        vo.setAlertThreshold(po.getAlertThreshold());
        return vo;
    }
}
