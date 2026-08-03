package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉分页查询入参，支持按状态和订单号过滤
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class ComplaintPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;

    /** 按状态过滤，null 不限制 */
    private Integer status;

    /** 按订单号模糊匹配，null 不限制 */
    private String orderNo;
}
