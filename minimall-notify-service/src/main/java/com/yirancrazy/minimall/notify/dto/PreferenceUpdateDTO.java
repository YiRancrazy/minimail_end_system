package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好更新 DTO，单条 (类别, 渠道, 开关) 三元组
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class PreferenceUpdateDTO {

    /** 通知类别编码，必须在 NotifyCategoryEnum 范围内 */
    @NotBlank(message = "categoryCode cannot be blank")
    private String categoryCode;

    /** 通知渠道：SITE/SMS/EMAIL */
    @NotBlank(message = "channel cannot be blank")
    @Pattern(regexp = "^(SITE|SMS|EMAIL)$", message = "channel must be SITE/SMS/EMAIL")
    private String channel;

    /** 是否启用 0=关 1=开 */
    @NotNull(message = "enabled cannot be null")
    private Integer enabled;
}
