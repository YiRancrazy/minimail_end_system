package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载平台发布全站公告所需的标题和正文，由公告接口校验后写入一条全端通知占位记录.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class AnnouncementCreateDto {
  @NotBlank(message = "公告标题不能为空")
  @Size(max = 128, message = "标题最长128字")
  private String title;

  @NotBlank(message = "公告内容不能为空")
  @Size(max = 4000, message = "内容最长4000字")
  private String content;
}
