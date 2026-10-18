package com.yirancrazy.minimall.order.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import com.yirancrazy.minimall.order.entity.OrderPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long skuId;

    private String skuName;
    private Integer quantity;

    private String amount;

    private String status;

    private String addressSnapshot;
    private LocalDateTime createTime;
    private LocalDateTime shippedAt;

    private String rejectReason;

    private List<OrderItemVO> items;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static OrderVO from(OrderPO po) {
        OrderVO vo = new OrderVO();
        vo.setId(po.getId());
        vo.setOrderNo(po.getOrderNo());
        vo.setUserId(po.getUserId());
        vo.setMerchantId(po.getMerchantId());
        vo.setSkuId(po.getSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setAmount(po.getAmount() == null ? null : po.getAmount().toPlainString());
        vo.setStatus(statusAlias(po.getStatus()));
        vo.setAddressSnapshot(po.getReceiverSnapshotJson());
        vo.setCreateTime(po.getCreateTime());
        vo.setShippedAt(po.getShippedAt());
        return vo;
    }


    /**
     * 处理状态Alias。
     * @param code 编码
     * @return 处理结果
     */
    private static String statusAlias(Integer code) {
        for (com.yirancrazy.minimall.order.constant.OrderStatusEnum e
                : com.yirancrazy.minimall.order.constant.OrderStatusEnum.values()) {
            if (e.getCode().equals(String.valueOf(code))) {
                return e.getAlias();
            }
        }
        return String.valueOf(code);
    }
}
