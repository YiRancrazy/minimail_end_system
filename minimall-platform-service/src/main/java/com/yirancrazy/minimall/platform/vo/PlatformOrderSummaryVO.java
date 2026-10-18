package com.yirancrazy.minimall.platform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformOrderSummaryVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOrderSummaryVO {

    private Long paid;
    private Long pending;
    private Long refunding;
    private Long shipped;
    private Long completed;
    private Long cancelled;
}
