package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 描述单个通知类别在 SITE、SMS 或 EMAIL 渠道上的开关值，服务层会将任意非零值归一化为启用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class PreferenceUpdateDto {

    @NotBlank(message = "categoryCode cannot be blank")
    private String categoryCode;

    @NotBlank(message = "channel cannot be blank")
    @Pattern(regexp = "^(SITE|SMS|EMAIL)$", message = "channel must be SITE/SMS/EMAIL")
    private String channel;

    @NotNull(message = "enabled cannot be null")
    private Integer enabled;
}
