package com.yirancrazy.minimall.order.dto;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货人信息DTO，下单时序列化为快照落库，供订单详情与商家发货回显。
 * @Version: 1.0
 * @DateTime: 2026/08/27
 */
@Data
public class ReceiverDTO {
    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detailAddress;
}