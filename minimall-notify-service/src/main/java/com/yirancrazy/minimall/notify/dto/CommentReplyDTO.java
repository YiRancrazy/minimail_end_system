package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentReplyDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class CommentReplyDTO {


    @NotBlank(message = "reply cannot be blank")
    @Size(max = 1024, message = "reply length must be <= 1024")
    private String reply;
}
