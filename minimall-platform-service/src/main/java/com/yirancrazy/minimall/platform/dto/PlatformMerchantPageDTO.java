package com.yirancrazy.minimall.platform.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantPageDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PlatformMerchantPageDTO extends CursorPageDTO {


    private String keyword;


    private Integer auditStatus;
}
