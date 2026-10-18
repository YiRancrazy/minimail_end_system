package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceUpdateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class PreferenceUpdateDTO {


    @NotBlank(message = "categoryCode cannot be blank")
    private String categoryCode;


    @NotBlank(message = "channel cannot be blank")
    @Pattern(regexp = "^(SITE|SMS|EMAIL)$", message = "channel must be SITE/SMS/EMAIL")
    private String channel;


    @NotNull(message = "enabled cannot be null")
    private Integer enabled;
}
