package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台向指定商家发送违规警告所需的商家编号、标题、正文和违规类型.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifyViolationWarningDto {

    @NotNull(message = "merchantId cannot be null")
    private Long merchantId;

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    private String violationType;
}
