package com.yirancrazy.minimall.order.vo;

import java.time.LocalDateTime;
import java.util.List;
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
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private Long skuId;
    /** 商品名，商家端列表由 skuId 批量装配，查询失败时为 null */
    private String skuName;
    private Integer quantity;
    /** 订单金额（元），字符串避免前端浮点精度问题 */
    private String amount;
    /** 订单状态枚举别名（如 PENDING），未知状态码原样返回 */
    private String status;
    /** 收货地址快照（JSON 字符串，由下单时地址序列化而来） */
    private String addressSnapshot;
    private LocalDateTime createTime;
    private LocalDateTime shippedAt;
    /** 商家驳回退款原因，来自状态日志；未驳回时为 null */
    private String rejectReason;
    /** 订单商品明细，用户端详情页展示 */
    private List<OrderItemVO> items;

    /**
     * 将 OrderPO 转换为 OrderVO。
     * @param po 订单持久化对象
     * @return 订单VO
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
     * 将持久化状态码映射为枚举别名，未知码原样返回数字字符串避免丢失状态。
     * @param code 持久化状态码
     * @return 状态枚举别名
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
