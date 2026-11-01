package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载商家或平台回复评价时提交的回复正文.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CommentReplyDto {

    @NotBlank(message = "reply cannot be blank")
    @Size(max = 1024, message = "reply length must be <= 1024")
    private String reply;
}
