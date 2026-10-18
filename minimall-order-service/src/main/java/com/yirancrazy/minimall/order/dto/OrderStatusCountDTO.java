package com.yirancrazy.minimall.order.dto;

import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusCountDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class OrderStatusCountDTO {


    private Integer status;


    private Long cnt;
}
