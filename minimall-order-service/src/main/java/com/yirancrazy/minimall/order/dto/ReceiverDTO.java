package com.yirancrazy.minimall.order.dto;

import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ReceiverDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class ReceiverDTO {

    private String receiverName;


    private String receiverPhone;


    private String province;


    private String city;


    private String district;


    private String detailAddress;
}