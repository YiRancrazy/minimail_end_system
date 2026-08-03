package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统告警通知入参，向平台管理员发送系统异常/故障预警站内信
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifySystemAlertDTO {

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    /** 告警级别，如 INFO/WARN/CRITICAL */
    private String alertLevel;
}
