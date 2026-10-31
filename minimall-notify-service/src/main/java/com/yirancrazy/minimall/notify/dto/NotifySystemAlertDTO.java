package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台系统告警的标题、正文和告警级别，用于生成供平台端查询的系统告警消息.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifySystemAlertDto {

  @NotBlank(message = "title cannot be blank")
  private String title;

  @NotBlank(message = "content cannot be blank")
  private String content;

  private String alertLevel;
}
