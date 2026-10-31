package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台营销通知的标题、正文和可选目标用户列表；列表为空时保存全端营销占位消息.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifyMarketingPushDto {

  @NotBlank(message = "title cannot be blank")
  private String title;

  @NotBlank(message = "content cannot be blank")
  private String content;

  private List<Long> userIds;
}
