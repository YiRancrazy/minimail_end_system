package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyViolationWarningDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class NotifyViolationWarningDTO {

    @NotNull(message = "merchantId cannot be null")
    private Long merchantId;

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;


    private String violationType;
}
