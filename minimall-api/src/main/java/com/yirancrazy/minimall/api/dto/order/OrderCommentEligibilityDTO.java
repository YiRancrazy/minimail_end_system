package com.yirancrazy.minimall.api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderCommentEligibilityDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommentEligibilityDTO {


    private boolean eligible;

    private Long orderId;

    private Long userId;


    private Long merchantId;

    private Integer status;
}
