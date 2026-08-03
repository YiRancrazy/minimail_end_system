package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉创建 DTO，用于提交投诉入参校验
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class ComplaintCreateDTO {

    @NotNull(message = "complainantType cannot be null")
    private Integer complainantType;

    /** 投诉方ID，由 Controller 从 X-User-Id 覆盖，DTO 中仅作占位 */
    private Long complainantId;

    @NotNull(message = "defendantType cannot be null")
    private Integer defendantType;

    @NotNull(message = "defendantId cannot be null")
    private Long defendantId;

    @Size(max = 64, message = "orderNo length must be <= 64")
    private String orderNo;

    @NotBlank(message = "complaintType cannot be blank")
    @Size(max = 32, message = "complaintType length must be <= 32")
    private String complaintType;

    @NotBlank(message = "title cannot be blank")
    @Size(max = 128, message = "title length must be <= 128")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;
}
