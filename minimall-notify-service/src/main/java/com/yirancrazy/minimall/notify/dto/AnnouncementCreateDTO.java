package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 公告发布请求体，平台发布系统公告。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class AnnouncementCreateDTO {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 128, message = "标题最长128字")
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 4000, message = "内容最长4000字")
    private String content;
}
