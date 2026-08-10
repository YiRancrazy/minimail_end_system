package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家回复评价 DTO
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class CommentReplyDTO {

    /** 回复内容 */
    @NotBlank(message = "reply cannot be blank")
    @Size(max = 1024, message = "reply length must be <= 1024")
    private String reply;
}
