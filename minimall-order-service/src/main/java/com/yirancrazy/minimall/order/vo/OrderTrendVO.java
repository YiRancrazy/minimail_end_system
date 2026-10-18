package com.yirancrazy.minimall.order.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderTrendVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrendVO {

    private List<OrderTrendPointVO> points;
}
