package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.order.entity.OrderPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class OrderVO {

    private Long id;
    private Long userId;
    private Long merchantId;
    private Long skuId;
    private Integer quantity;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;

    /**
     * 将 OrderPO 转换为 OrderVO。
     * @param po 订单持久化对象
     * @return 订单VO
     */
    public static OrderVO from(OrderPO po) {
        OrderVO vo = new OrderVO();
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
