package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 违规警告通知入参，向指定商家发送违规警告站内信
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifyViolationWarningDTO {

    @NotNull(message = "merchantId cannot be null")
    private Long merchantId;

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    /** 违规类型，如假货/刷单/超卖等 */
    private String violationType;
}
