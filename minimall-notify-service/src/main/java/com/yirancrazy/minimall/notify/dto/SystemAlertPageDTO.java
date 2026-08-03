package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统告警分页查询入参，固定查询 PLATFORM + SYSTEM 类型消息
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class SystemAlertPageDTO {

    @Min(value = 1, message = "pageNo must be >= 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize must be >= 1")
    @Max(value = 100, message = "pageSize must be <= 100")
    private Integer pageSize = 20;
}
