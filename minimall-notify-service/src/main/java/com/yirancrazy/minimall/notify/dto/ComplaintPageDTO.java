package com.yirancrazy.minimall.notify.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ComplaintPageDTO extends CursorPageDTO {


    private Integer status;


    private String orderNo;


    private Long userId;


    private Long defendantId;
}
