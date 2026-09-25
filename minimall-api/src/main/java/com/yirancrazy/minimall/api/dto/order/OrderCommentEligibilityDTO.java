package com.yirancrazy.minimall.api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单评价资格 DTO，承载评价创建前的归属、商品与购买状态校验结果及订单快照商家ID。
 * @Version: 1.0
 * @DateTime: 2026/09/25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommentEligibilityDTO {

    /** 是否具备评价资格：订单归属、订单商品、购买状态三项同时满足 */
    private boolean eligible;

    private Long orderId;

    private Long userId;

    /** 订单快照中的商家ID，评价落库以该值为准 */
    private Long merchantId;

    private Integer status;
}
