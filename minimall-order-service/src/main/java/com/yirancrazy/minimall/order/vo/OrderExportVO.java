package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.order.entity.OrderPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderExportVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderExportVO {

    private Long id;
    private Long userId;
    private Long merchantId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static OrderExportVO from(OrderPO po) {
        OrderExportVO vo = new OrderExportVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setMerchantId(po.getMerchantId());
        vo.setSkuId(po.getSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setAmount(po.getAmount());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
