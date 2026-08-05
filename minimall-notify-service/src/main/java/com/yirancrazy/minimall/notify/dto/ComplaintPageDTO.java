package com.yirancrazy.minimall.notify.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉分页查询入参，支持按状态和订单号过滤
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ComplaintPageDTO extends CursorPageDTO {

    /** 按状态过滤，null 不限制 */
    private Integer status;

    /** 按订单号模糊匹配，null 不限制 */
    private String orderNo;
}
